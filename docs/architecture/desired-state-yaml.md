# Architektur — Desired-State-YAML (Format & Funktionsweise)

Status: Draft
Bezug: [ADR-0019](../decisions/0019-git-config-repository-source.md),
[ADR-0001](../decisions/0001-git-as-single-source-of-truth.md),
[config-repository](../domain/config-repository.md), [reconciliation-loop](reconciliation-loop.md)

D4Y liest den **Sollzustand** — welche Anwendungen mit welchem Image, welchen Volumes und Routes
laufen sollen — aus deklarativen **YAML-Dateien**. Dieses Dokument beschreibt deren **Struktur** und
wie D4Y sie **verarbeitet**.

> **Bezugsquelle ([ADR-0019](../decisions/0019-git-config-repository-source.md)):** Ist eine
> `d4y.config-repo.url` gesetzt, klont D4Y das **Git-Config-Repository** und aktualisiert es
> periodisch (read-only Spiegel); die YAML-Dateien werden aus dem Klon gelesen. Ohne URL greift der
> **lokale Fallback** (`d4y.desired-state.path`, Default `./desired`) — praktisch für Entwicklung.
> Format und Verarbeitung sind in beiden Fällen identisch; der Sollzustand ist **read-only für UI
> und API** ([ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)) — Änderungen erfolgen
> ausschließlich über die Dateien (im Git-Modus über Commits).
>
> **Voll-Push-Modus ([ADR-0025](../decisions/0025-full-push-desired-state-delivery.md)):** Ohne
> gesetzte `d4y.config-repo.url` kann eine externe Pipeline (GitHub Actions) die YAML-Dateien per
> authentifiziertem `POST /api/reconcile` **liefern**; d4y schreibt sie ins Desired-Verzeichnis und
> liest sie wie hier beschrieben. Damit zieht d4y nichts und hält keine GitHub-Credentials. Der
> `POST /api/reconcile`-Push ist der einzige Schreibweg von außen und ändert den Sollzustand — die
> übrige API bleibt read-only.

## Ablage & Erkennung

- **Git-Modus:** Verzeichnis = `<klon>/<d4y.config-repo.path>` (Default: Repo-Wurzel).
  **Lokaler Modus:** `d4y.desired-state.path` (Default `./desired`).
- Alle Dateien mit Endung `*.yaml` oder `*.yml` werden gelesen, in **sortierter** Reihenfolge.
- Existiert das Verzeichnis nicht (z. B. Klon noch nicht da), gilt ein **leerer** Sollzustand
  (kein Fehler).

## Dateiform

Jede Datei enthält **entweder** ein einzelnes Application-Objekt **oder** eine **YAML-Liste**
solcher Objekte. Beide Formen sind gleichwertig; die Aufteilung auf Dateien ist frei wählbar.

## Felder

| Feld      | Pflicht | Typ           | Bedeutung |
| --------- | ------- | ------------- | --------- |
| `name`    | ja      | String        | Name der [Application](../domain/application.md); darf nicht leer sein. Der Container heißt `d4y_<name>`. |
| `image`   | ja      | String        | Unveränderliche Image-Referenz `repository:tag` ([ADR-0002](../decisions/0002-immutable-images-no-build-on-target.md)); darf nicht leer sein. |
| `volumes` | nein    | Liste Objekte | Deklarierte **Named Volumes** der App (siehe unten). Fehlt das Feld, hat die App keine Volumes. |
| `routes`  | nein    | Liste Objekte | Deklarierter **externer Ingress** (Hostname → App, siehe unten). Fehlt das Feld, hat die App keine Routes. |
| `env`     | nein    | Map           | Deklarierte **Umgebungsvariablen** (Key→Value), die dem Container gesetzt werden. Teil des Sollzustands (Änderung ⇒ Replace). |
| `backup`  | nein    | Bool          | Aktiviert **Backup/Restore** der Named Volumes ([ADR-0020](../decisions/0020-backup-restore-s3-rclone.md)). Default `false` ⇒ App ist ephemer. |

### `volumes[]` — Named Volumes

Jeder Eintrag beschreibt ein **Named Volume** (engine-verwaltet, kein Host-Pfad — der Standard für
persistente Daten; **Bind Mounts sind hier nicht vorgesehen**, siehe
[volume](../domain/volume.md) und [ADR-0009](../decisions/0009-persistence-optional-backup-restore.md)).

| Feld   | Pflicht | Typ    | Bedeutung |
| ------ | ------- | ------ | --------- |
| `name` | ja      | String | Logischer Volume-Name. Muster: `[a-zA-Z0-9][a-zA-Z0-9_.-]*`. Der Engine-Volume-Name lautet `d4y_<app>_<name>`. |
| `path` | ja      | String | **Absoluter** Mount-Pfad im Container (z. B. `/var/lib/data`). |

Regeln:

- `name` **und** `path` sind je Eintrag erforderlich.
- Innerhalb einer App müssen sowohl die `name`- als auch die `path`-Werte **eindeutig** sein
  (Duplikate werden abgelehnt).
- Nur die **Deklaration** ist Teil des Sollzustands, **nicht** der Volume-**Inhalt**
  ([desired-vs-actual-state](../domain/desired-vs-actual-state.md)).

### `routes[]` — externer Ingress

Jeder Eintrag ordnet einen **Hostnamen** (und ggf. Pfad) der App zu — externer Ingress
([route](../domain/route.md), [ADR-0010](../decisions/0010-dns-ingress-service-discovery.md)).

| Feld   | Pflicht | Typ     | Bedeutung |
| ------ | ------- | ------- | --------- |
| `host` | ja      | String  | Hostname, unter dem die App erreichbar sein soll (z. B. `web.example.com`); ohne Schema/Slash. |
| `path` | nein    | String  | Pfad-Präfix; muss mit `/` beginnen. Default `/`. |
| `port` | nein    | Zahl    | Ziel-Port im Container, an den der Reverse Proxy weiterreicht. Default `80`. |
| `tls`  | nein    | Boolean | TLS für diese Route ([ADR-0028](../decisions/0028-per-route-tls-and-http-mode.md)): `true` = HTTPS (`websecure`), `false` = reines HTTP (`web`). Ohne Angabe gilt der globale Default (aus ACME abgeleitet). |

> **Umsetzungsstand ([ADR-0016](../decisions/0016-reverse-proxy-traefik-docker-labels.md),
> [ADR-0028](../decisions/0028-per-route-tls-and-http-mode.md)):** Routes werden über einen von D4Y
> verwalteten **Traefik**-Reverse-Proxy angewandt (Docker-Provider). D4Y rendert je Route
> Traefik-Router/Service-Labels auf den App-Container und hängt alle verwalteten Container an ein
> gemeinsames `d4y`-Netz. Weil diese Labels beim Erstellen gebacken werden, ist eine geänderte
> Route-Deklaration (inkl. `tls`) **Drift** und führt zu einem Container-**Replace**.
> **TLS ist pro Route schaltbar:** `tls: true` → `websecure :443` mit TLS (bei ACME `certresolver=le`,
> sonst self-signed), `tls: false` → `web :80` reines HTTP. Ohne ACME-Mail ist der Default HTTP-only
> (VM/Intranet ohne öffentliche IP). Es gibt **keinen** automatischen HTTP→HTTPS-Redirect. Die
> DNS-Modi (managed/extern) folgen später — siehe [networking-and-dns](networking-and-dns.md).

## Beispiel

Einzelne App mit zwei Named Volumes (`desired/nginx.yaml`):

```yaml
name: nginx
image: nginx:1.27-alpine
volumes:
  - name: html
    path: /usr/share/nginx/html
  - name: cache
    path: /var/cache/nginx
routes:
  - host: nginx.example.com        # TLS = globaler Default
  - host: api.example.com
    path: /v1
    port: 8080
    tls: true                      # diese Route explizit HTTPS
env:
  LOG_LEVEL: info
  TZ: Europe/Berlin
backup: true
```

> **env & operative Overrides:** Das deklarierte `env` ist Sollzustand. Die operative Aktion
> *„Temporäre Parameter"* ([operational-actions](operational-actions.md)) legt darüber einen
> **transienten Override** (unter einem zeitlich begrenzten Hold); nach Ablauf des Holds kehrt der
> Reconcile zum deklarierten `env` zurück. Werte können Geheimnisse enthalten und werden in
> API/UI **nicht** ausgegeben (nur Schlüssel) — [privacy-rules](../rules/privacy-rules.md).

### Secret-Platzhalter `${secret:NAME}`

In `env`-Werten und im Registry-Passwort darf ein **Platzhalter** `${secret:NAME}` stehen, statt eines
Klartext-Geheimnisses. Der Wert liegt **nicht** im Repo, sondern wird extern gehalten (z. B.
GitHub-Actions-Secrets) und per authentifiziertem **Push** an d4y geliefert
([ADR-0023](../decisions/0023-push-triggered-reconcile-and-trigger-auth.md),
[ADR-0024](../decisions/0024-delivered-image-secrets-encrypted-store.md)). Beim Laden des Sollzustands
löst d4y die Platzhalter aus seinem verschlüsselten Secret-Store auf. Ein **unauflösbarer** Platzhalter
ist ein Fehler: die betroffene App wird übersprungen und auditiert — es gelangt nie ein Platzhalter an
die Engine.

```yaml
name: private-app
image: registry.example.com/team/app:1.4.2   # private Registry
env:
  API_TOKEN: ${secret:APP_API_TOKEN}
```

Mehrere Apps in einer Datei (Listen-Form):

```yaml
- name: web
  image: nginx:1.27-alpine
- name: cache
  image: redis:7
  volumes:
    - name: data
      path: /data
```

## Funktionsweise

Der [Reconciliation-Loop](reconciliation-loop.md) läuft periodisch
(`d4y.reconcile.interval-ms`, Default 15 000 ms) nach dem Muster **observe → diff → reconcile**:

1. **Laden (Soll):** Alle YAML-Dateien werden zum aktuellen Sollzustand eingelesen.
2. **Beobachten (Ist):** D4Y listet die von ihm verwalteten Container (Label `d4y.managed=true`)
   und liest deren Labels `d4y.app`, `d4y.image`, `d4y.volumes`, `d4y.routes`.
3. **Abgleich:** Je deklarierter App wird verglichen. Ein Container **passt**, wenn er läuft **und**
   mit derselben Image-Referenz, Volume- **und** Route-Deklaration erzeugt wurde. Andernfalls:
   - kein Container vorhanden → **Start**,
   - Container vorhanden, weicht aber ab (Image, Running-Status **oder** Volumes) → **Replace**
     (stoppen/entfernen, neu erstellen),
   - passt → **Noop**.
   - Verwalteter Container **ohne** Deklaration → **StopAndRemove** (nicht deklarierte Drift; im
     Frontend als „Undeclared" sichtbar).
4. **Named Volumes:** Vor dem Erstellen wird jedes deklarierte Volume **idempotent** angelegt
   (`d4y_<app>_<name>`) und am `path` in den Container gemountet. Beim **Replace** bleibt das Named
   Volume bestehen — die **Daten überleben** den Container-Austausch. Ohne konfiguriertes Backup
   sind die Daten dennoch **ephemer** (Backup ist derzeit nicht implementiert).
5. **Labels:** Neu erstellte Container werden mit `d4y.managed`, `d4y.app`, `d4y.image` und
   `d4y.volumes` (kodierte Deklaration) gestempelt — Grundlage für den Drift-Vergleich im nächsten
   Durchlauf.

Ein aktiver, zeitlich begrenzter [Reconciliation-Hold](../domain/reconciliation-hold.md) setzt den
Abgleich für ein Ziel vorübergehend aus (weder Replace noch StopAndRemove).

## Grenzen & Hinweise

- Der **Inhalt** persistenter Volumes ist **nicht** Teil des Sollzustands; Durability entsteht
  ausschließlich über ein (noch nicht implementiertes) [Backup](../domain/backup.md).
- UI und API sind **read-only** bezüglich des Sollzustands; jede Änderung erfolgt über die Dateien.
- Die lokale Bezugsquelle ist **interim** und endet mit der Git-Anbindung (Sunset in
  [ADR-0011](../decisions/0011-interim-local-desired-state-source.md)).

## Konfiguration

| Schlüssel                          | Default     | Bedeutung |
| ---------------------------------- | ----------- | --------- |
| `d4y.config-repo.url`              | *(leer)*    | Git-URL des Config-Repos. Gesetzt ⇒ Git-Modus; leer ⇒ lokaler Fallback. |
| `d4y.config-repo.branch`           | `main`      | Branch/Ref. |
| `d4y.config-repo.path`             | *(leer)*    | Unterpfad im Repo mit den YAML-Dateien (leer = Wurzel). |
| `d4y.config-repo.poll-interval-ms` | `30000`     | Intervall für `fetch`/`reset` des Klons. |
| `d4y.config-repo.username` / `.token` | *(leer)* | HTTPS-Zugangsdaten für private Repos (Geheimnis). |
| `d4y.desired-state.path`           | `./desired` | YAML-Verzeichnis im **lokalen** Fallback. |
| `d4y.reconcile.interval-ms`        | `15000`     | Intervall des Reconciliation-Loops in Millisekunden. |
| `d4y.backup.interval-ms`           | `300000`    | Intervall des periodischen Backups. |
| `d4y.backup.s3.endpoint` / `.bucket` | *(leer)*  | S3-Backup-Store. Gesetzt ⇒ Backup aktiv (App opt-in via `backup: true`). |
| `d4y.backup.s3.region` / `.provider` | `us-east-1` / `Other` | S3-Region und rclone-Provider (z. B. `Minio`, `AWS`). |
| `d4y.backup.s3.access-key` / `.secret-key` | *(leer)* | S3-Zugangsdaten (Geheimnisse). |
| `d4y.ingress.https-redirect`       | `true`      | HTTP→HTTPS-Redirect am Reverse Proxy. |
| `d4y.ingress.internal-domain`      | `d4y.internal` | Interne DNS-Domain für Service-Discovery-Aliase (`<app>.<domain>`). |
| `d4y.ingress.dns-mode`             | `extern`    | Öffentliche DNS-Verwaltung: `extern` (D4Y fasst DNS nicht an) oder `managed` (Platzhalter). |
| `d4y.ingress.tls.acme.email`       | *(leer)*    | Gesetzt ⇒ ACME/Let's-Encrypt aktiv; leer ⇒ self-signed Default-Zertifikat. |
| `d4y.ingress.tls.acme.challenge`   | `http`      | ACME-Challenge: `http` oder `dns`. |
| `d4y.ingress.tls.acme.dns-provider`| *(leer)*    | Traefik-DNS-Provider (nur bei `challenge=dns`). |
| `d4y.ingress.tls.acme.env.*`       | *(leer)*    | Zugangsdaten des DNS-Providers als Traefik-Container-Env (Geheimnisse). |
| `d4y.ingress.tls.acme.ca-server`   | *(leer)*    | Optionaler alternativer ACME-CA-Server (z. B. Staging). |
| `d4y.trigger.token`                | *(leer)*    | Bearer-Token für `POST /api/reconcile` (host-Credential). Leer ⇒ Endpoint deaktiviert ([ADR-0023](../decisions/0023-push-triggered-reconcile-and-trigger-auth.md)). |
| `d4y.secrets.encryption-key`       | *(leer)*    | Schlüssel für den AES-GCM-verschlüsselten Secret-Store (host-Credential). Leer ⇒ gelieferte Secrets werden nur im RAM gehalten ([ADR-0024](../decisions/0024-delivered-image-secrets-encrypted-store.md)). |
| `d4y.secrets.file`                 | `./.d4y-secrets` | Ablageort des verschlüsselten Secret-Stores. |

## Referenzen

- [ADR-0011 — Interim lokale Desired-State-Quelle](../decisions/0011-interim-local-desired-state-source.md)
- [ADR-0001 — Git als einzige Quelle der Wahrheit](../decisions/0001-git-as-single-source-of-truth.md)
- [ADR-0002 — Unveränderliche Images](../decisions/0002-immutable-images-no-build-on-target.md)
- [ADR-0009 — Persistenz: optionales Backup/Restore](../decisions/0009-persistence-optional-backup-restore.md)
- [Domäne — Application](../domain/application.md) · [Volume](../domain/volume.md) ·
  [Config-Repository](../domain/config-repository.md)
- [Architektur — Reconciliation-Loop](reconciliation-loop.md)
