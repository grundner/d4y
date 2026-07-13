# Beispiel: d4y Config-Repo (Docker Compose)

Ein vollständiges Beispiel-Config-Repo nach dem Compose-Modell
([ADR-0029](../../docs/decisions/0029-docker-compose-single-source-format.md)) und Voll-Push
([ADR-0025](../../docs/decisions/0025-full-push-desired-state-delivery.md)): **je App ein Verzeichnis**
mit einer standardkonformen `compose.yaml` und einer d4y-Sidecar `d4y.yaml`. Eine GitHub-Actions-
Pipeline pusht die App-Verzeichnisse an d4y (`POST /api/reconcile`); d4y führt sie über
`docker compose` aus und hält den Zustand kontinuierlich.

> In ein **eigenes** Repo kopieren, anpassen, `git push`. In diesem d4y-Repo läuft die enthaltene
> Pipeline nicht (verschachtelte `.github/workflows` ignoriert GitHub Actions).

## Struktur

```
web/       compose.yaml + d4y.yaml            # oeffentliche App (nginx), HTTP-Route
api/       compose.yaml + d4y.yaml            # App mit Volume, Secret (${VAR}), HTTPS-Route, Backup
builder/   compose.yaml + Dockerfile + d4y.yaml  # Bauen auf dem Ziel (build:)
.github/workflows/deploy.yml                  # Auslieferungs-Pipeline
```

- **`compose.yaml`** ist reines, valides Compose (lokal mit `docker compose up` lauffähig) — **keine**
  d4y-Spezifika.
- **`d4y.yaml`** (Sidecar) trägt Routes/Ingress und Backup-Opt-in. d4y erzeugt daraus ein Override,
  hängt die Services ans `d4y`-Netz und injiziert die Traefik-Labels.

## Voraussetzungen

1. **Ein laufender d4y-Host** (1-Zeiler-Installer). Ohne ACME-Mail läuft alles über HTTP.
2. **GitHub-Actions-Secrets** im Config-Repo:
   | Secret | Wert |
   | --- | --- |
   | `D4Y_URL` | `https://d4y.example.com` bzw. `http://…` |
   | `D4Y_TRIGGER_TOKEN` | Token aus der Installer-Ausgabe |
   | `APP_API_TOKEN` | Wert für `${APP_API_TOKEN}` (von `api/compose.yaml`) |

   Für **jede** weitere `${VAR}` in einer `compose.yaml` ein gleichnamiges Secret anlegen **und** in
   `deploy.yml` durchreichen.

## Wie es funktioniert

1. `deploy.yml` sammelt alle Dateien der App-Verzeichnisse als `{ "<relpfad>": "<inhalt>" }`.
2. Es sendet `{ config, secrets }` per `POST <D4Y_URL>/api/reconcile` (Bearer-Token).
3. d4y schreibt die App-Verzeichnisse, übergibt die Secrets als **Prozess-Env** an `docker compose`
   (native `${VAR}`-Interpolation, nie auf Platte) und führt je App
   `docker compose -p d4y-<app> up -d` aus. Idempotent/self-healing; undeklarierte Projekte werden
   entfernt.

## Secrets

Geheimnisse stehen **nie** im Repo. In der `compose.yaml` werden sie als `${VAR}` referenziert; d4y
löst sie aus seinem verschlüsselten Store auf und übergibt sie als Env an den Compose-Prozess.

## Hinweis (macOS-Clients)

Keine `.local`-Hostnamen verwenden — macOS leitet `.local` an mDNS und ignoriert `/etc/hosts`. Nutze
z. B. `.test` und trage den Host in `/etc/hosts` auf die Host-/VM-IP ein.

## Referenzen

- [Compose-App-Format](../../docs/architecture/compose-app-format.md)
- [ADR-0029 — Docker Compose als einziges Quellformat](../../docs/decisions/0029-docker-compose-single-source-format.md)
