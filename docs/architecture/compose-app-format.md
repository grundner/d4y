# Architektur — Sollzustand als Docker-Compose-Projekte

Status: Draft
Bezug: [ADR-0029](../decisions/0029-docker-compose-single-source-format.md),
[ADR-0007](../decisions/0007-continuous-reconciliation-self-healing.md),
[ADR-0016](../decisions/0016-reverse-proxy-traefik-docker-labels.md),
[ADR-0028](../decisions/0028-per-route-tls-and-http-mode.md),
[config-repository](../domain/config-repository.md), [reconciliation-loop](reconciliation-loop.md)

Der Sollzustand von d4y besteht aus **Docker-Compose-Projekten** — **je App ein Verzeichnis**
([ADR-0029](../decisions/0029-docker-compose-single-source-format.md)). d4y führt sie über die
**`docker compose`-CLI** aus und gleicht sie kontinuierlich mit dem Ist-Zustand ab (Self-Healing).
Dieses Dokument beschreibt das Format und die Verarbeitung. (Das frühere proprietäre Einzel-YAML
[`desired-state-yaml.md`](desired-state-yaml.md) ist damit abgelöst.)

## Verzeichnislayout

```text
apps/
  web/
    compose.yaml          # PFLICHT: standardkonformes, valides Compose
    d4y.yaml              # optional: d4y-Sidecar (Routes/Ingress, Backup)
  api/
    compose.yaml
    d4y.yaml
    Dockerfile            # optional: bei build:
    .env                  # optional: von Compose automatisch geladen
```

- **App = Unterverzeichnis** mit einer `compose.yaml` (oder `docker-compose.yml`). Der App-Name ist
  der Verzeichnisname; das Compose-Projekt heißt `d4y-<app>`.
- Die `compose.yaml` bleibt **herstellerneutral** und lokal mit `docker compose up` lauffähig — sie
  enthält **keine** d4y-Spezifika.
- Begleitdateien (`Dockerfile` + Build-Kontext, `.env`, `env_file`, Config-Dateien) liegen im
  App-Verzeichnis.

## `compose.yaml`

Standard-Compose. Erlaubt ist der übliche Umfang inkl. `build:` (Bauen auf dem Ziel,
[ADR-0029](../decisions/0029-docker-compose-single-source-format.md)), mehrerer Services, `depends_on`,
Healthchecks, Volumes, `env_file` usw. d4y ändert die Datei nicht; es fügt zur Laufzeit nur ein
**Override** hinzu (siehe unten).

Empfehlung: für schwere/binäre Build-Kontexte besser ein **vorgebautes Image** aus einer Registry
beziehen statt auf dem Ziel zu bauen.

## `d4y.yaml` (Sidecar, optional)

Trägt die d4y-spezifischen Belange, damit die `compose.yaml` sauber bleibt.

```yaml
# Externer Ingress: welche Compose-Services unter welchem Host erreichbar sind.
routes:
  - service: web            # PFLICHT: Compose-Service, der exponiert wird
    host: web.example.com   # PFLICHT: externer Hostname (ohne Schema/Slash)
    port: 80                # optional: Ziel-Port im Service (Container-Port). Default 80
    path: /                 # optional: Pfad-Präfix. Default /
    tls: true               # optional: true=HTTPS, false=HTTP. Ohne Angabe globaler Default (ADR-0028)

# Backup-Opt-in: welche Compose-Volumes dieser App gesichert werden (ADR-0020).
backup:
  volumes: [data]           # Compose-Volume-Namen; leer/fehlt ⇒ kein Backup (App ephemer)
```

Fehlt `d4y.yaml`, hat die App **keinen** externen Ingress (nur interne Erreichbarkeit über das
`d4y`-Netz) und **kein** Backup.

## Verarbeitung

Der [Reconciliation-Loop](reconciliation-loop.md) ([ADR-0007](../decisions/0007-continuous-reconciliation-self-healing.md))
läuft periodisch (`d4y.reconcile.interval-ms`) nach **enumerate → override → apply → prune**:

1. **Enumerate:** alle App-Verzeichnisse mit einer `compose.yaml` unter dem Desired-Root.
2. **Override generieren:** aus `compose.yaml` (Service-Namen) + `d4y.yaml` schreibt d4y eine
   `d4y-override.yaml`:
   - `networks: { d4y: { external: true } }` und je Service `networks: [d4y]` (Beitritt zum von d4y
     gemanagten `d4y`-Netz, damit Traefik routet und interne Service-Discovery greift).
   - je `route` die **Traefik-Labels** am Ziel-Service (Router `Host(...)` [+ `PathPrefix`],
     Entrypoint/TLS gemäß [ADR-0028](../decisions/0028-per-route-tls-and-http-mode.md),
     `loadbalancer.server.port`). Alternativ darf der Nutzer Traefik-Labels direkt in `compose.yaml`
     setzen.
3. **Apply:** `docker compose -p d4y-<app> -f compose.yaml -f d4y-override.yaml up -d --remove-orphans`.
   - **Build nur bei Änderung:** Push-getriggert mit `--build`; der periodische Self-Heal-Heartbeat
     ohne `--build` (Idempotenz ohne Dauer-Rebuilds).
   - **Secrets:** gelieferte Secrets werden aufgelöst und als **Umgebungsvariablen** an den
     `docker compose`-Prozess übergeben; Compose interpoliert sie nativ (`${VAR}`/`env_file`). Secrets
     landen **nie im Klartext auf Platte** ([ADR-0024](../decisions/0024-delivered-image-secrets-encrypted-store.md)).
4. **Prune (Drift):** Compose-Projekte mit Label `com.docker.compose.project=d4y-<app>`, die **nicht**
   deklariert sind → `docker compose -p <projekt> down` (im Frontend als „Undeclared" sichtbar).

Voraussetzung: das **`docker compose`-Plugin** auf dem Host (der Installer stellt es sicher).

## Herkunft (Git / Push)

- **Git-Modus** ([ADR-0019](../decisions/0019-git-config-repository-source.md)): d4y klont das
  Config-Repo und liest die App-Verzeichnisse.
- **Voll-Push-Modus** ([ADR-0025](../decisions/0025-full-push-desired-state-delivery.md)): eine Pipeline
  liefert die App-Verzeichnisse als **Verzeichnisbaum** per `POST /api/reconcile`; d4y schreibt sie ins
  Desired-Verzeichnis (Traversal-geschützt) und reconciled. Format und Verarbeitung sind identisch.

## Konfiguration (Auszug)

| Schlüssel | Default | Bedeutung |
| --- | --- | --- |
| `d4y.desired-state.path` | `./desired` | Wurzel der App-Verzeichnisse (lokal/Push). |
| `d4y.config-repo.url` / `.branch` / `.path` | *(leer)* / `main` / *(leer)* | Git-Quelle des Config-Repos. |
| `d4y.reconcile.interval-ms` | `15000` | Intervall des Reconcile/Self-Heal. |
| `d4y.ingress.tls.default-enabled` | *(leer)* | Standard-TLS pro Route; leer ⇒ aus ACME abgeleitet ([ADR-0028](../decisions/0028-per-route-tls-and-http-mode.md)). |
| `d4y.ingress.tls.acme.*` | *(leer)* | ACME/Let's-Encrypt (Mail, Challenge, DNS-Provider). |
| `d4y.trigger.token` | *(leer)* | Bearer-Token für `POST /api/reconcile`. |
| `d4y.secrets.encryption-key` / `.file` | *(leer)* / `./.d4y-secrets` | Verschlüsselter Secret-Store. |
| `d4y.backup.s3.*` | *(leer)* | S3-Backup-Store (rclone); App-Opt-in via `d4y.yaml.backup`. |

## Referenzen

- [ADR-0029 — Docker Compose als einziges Quellformat](../decisions/0029-docker-compose-single-source-format.md)
- [ADR-0007 — Kontinuierliche Reconciliation / Self-Healing](../decisions/0007-continuous-reconciliation-self-healing.md)
- [ADR-0028 — Pro-Route-TLS und HTTP-Betrieb](../decisions/0028-per-route-tls-and-http-mode.md)
- [Reconciliation-Loop](reconciliation-loop.md) · [Networking & DNS](networking-and-dns.md)
