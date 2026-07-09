# Architektur — Desired-State-YAML (Format & Funktionsweise)

Status: Draft
Bezug: [ADR-0011](../decisions/0011-interim-local-desired-state-source.md),
[ADR-0001](../decisions/0001-git-as-single-source-of-truth.md),
[config-repository](../domain/config-repository.md), [reconciliation-loop](reconciliation-loop.md)

D4Y liest den **Sollzustand** — welche Anwendungen mit welchem Image und welchen Volumes laufen
sollen — aus deklarativen **YAML-Dateien**. Dieses Dokument beschreibt deren **Struktur** und wie
D4Y sie **verarbeitet**.

> **Interim (ADR-0011):** In der ersten Ausbaustufe stammen diese Dateien aus einem **lokalen
> Verzeichnis** statt aus einem geklonten Git-Repository. Format und Verarbeitung bleiben gleich,
> sobald die Git-Anbindung folgt; nur die Bezugsquelle ändert sich. Der Sollzustand ist und bleibt
> **read-only für UI und API** ([ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)) —
> Änderungen erfolgen ausschließlich über diese Dateien (später Git).

## Ablage & Erkennung

- Das Verzeichnis wird über die Konfiguration `d4y.desired-state.path` bestimmt (Default:
  `./desired`).
- Alle Dateien mit Endung `*.yaml` oder `*.yml` werden gelesen, in **sortierter** Reihenfolge.
- Existiert das Verzeichnis nicht, gilt ein **leerer** Sollzustand (kein Fehler).

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

| Feld   | Pflicht | Typ    | Bedeutung |
| ------ | ------- | ------ | --------- |
| `host` | ja      | String | Hostname, unter dem die App erreichbar sein soll (z. B. `web.example.com`); ohne Schema/Slash. |
| `path` | nein    | String | Pfad-Präfix; muss mit `/` beginnen. Default `/`. |
| `port` | nein    | Zahl   | Ziel-Port im Container, an den der Reverse Proxy weiterreicht. Default `80`. |

> **Umsetzungsstand ([ADR-0016](../decisions/0016-reverse-proxy-traefik-docker-labels.md)):** Routes
> werden über einen von D4Y verwalteten **Traefik**-Reverse-Proxy angewandt (Docker-Provider,
> HTTP `:80`). D4Y rendert je Route Traefik-Router/Service-Labels auf den App-Container und hängt
> alle verwalteten Container an ein gemeinsames `d4y`-Netz. Weil diese Labels beim Erstellen gebacken
> werden, ist eine geänderte Route-Deklaration **Drift** und führt zu einem Container-**Replace**.
> **HTTPS/TLS** ist aktiv ([ADR-0017](../decisions/0017-tls-https-ingress.md)): Routes werden auf
> `websecure :443` mit TLS bedient (Default self-signed, ACME opt-in). Die DNS-Modi (managed/extern)
> folgen später — siehe [networking-and-dns](networking-and-dns.md).

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
  - host: nginx.example.com
  - host: api.example.com
    path: /v1
    port: 8080
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
| `d4y.desired-state.path`           | `./desired` | Verzeichnis der YAML-Dateien. |
| `d4y.reconcile.interval-ms`        | `15000`     | Intervall des Reconciliation-Loops in Millisekunden. |
| `d4y.ingress.https-redirect`       | `true`      | HTTP→HTTPS-Redirect am Reverse Proxy. |
| `d4y.ingress.internal-domain`      | `d4y.internal` | Interne DNS-Domain für Service-Discovery-Aliase (`<app>.<domain>`). |
| `d4y.ingress.dns-mode`             | `extern`    | Öffentliche DNS-Verwaltung: `extern` (D4Y fasst DNS nicht an) oder `managed` (Platzhalter). |
| `d4y.ingress.tls.acme.email`       | *(leer)*    | Gesetzt ⇒ ACME/Let's-Encrypt aktiv; leer ⇒ self-signed Default-Zertifikat. |
| `d4y.ingress.tls.acme.challenge`   | `http`      | ACME-Challenge: `http` oder `dns`. |
| `d4y.ingress.tls.acme.dns-provider`| *(leer)*    | Traefik-DNS-Provider (nur bei `challenge=dns`). |
| `d4y.ingress.tls.acme.env.*`       | *(leer)*    | Zugangsdaten des DNS-Providers als Traefik-Container-Env (Geheimnisse). |
| `d4y.ingress.tls.acme.ca-server`   | *(leer)*    | Optionaler alternativer ACME-CA-Server (z. B. Staging). |

## Referenzen

- [ADR-0011 — Interim lokale Desired-State-Quelle](../decisions/0011-interim-local-desired-state-source.md)
- [ADR-0001 — Git als einzige Quelle der Wahrheit](../decisions/0001-git-as-single-source-of-truth.md)
- [ADR-0002 — Unveränderliche Images](../decisions/0002-immutable-images-no-build-on-target.md)
- [ADR-0009 — Persistenz: optionales Backup/Restore](../decisions/0009-persistence-optional-backup-restore.md)
- [Domäne — Application](../domain/application.md) · [Volume](../domain/volume.md) ·
  [Config-Repository](../domain/config-repository.md)
- [Architektur — Reconciliation-Loop](reconciliation-loop.md)
