# D4Y

**D4Y** ist eine **Git-first, Git-native Runtime Platform** für containerisierte Anwendungen.

Der gesamte Sollzustand der Infrastruktur wird deklarativ in einem Git-Repository beschrieben.
Server sind austauschbar und zustandslos. Anwendungen werden niemals auf dem Zielsystem gebaut,
sondern ausschließlich als unveränderliche Container-Images aus vertrauenswürdigen Registries
bereitgestellt. D4Y gleicht den Ist-Zustand kontinuierlich mit dem Soll-Zustand ab, korrigiert
Abweichungen automatisch und bleibt so dauerhaft selbstheilend.

> **Ziel:** Infrastruktur soll genauso reproduzierbar, versionierbar und deterministisch werden
> wie Software selbst.

## Status

**Dokumentationsgetrieben** — Änderungen beginnen in `docs/`; nur **Accepted** ADRs sind bindend.
Inzwischen existiert lauffähiger Code: ein Spring-Boot-Backend (`src/main/java/io/d4y/`) mit
Reconciliation-Loop, REST-API unter `/api` und eingebettetem Next.js-Frontend. Ein Teil der ADRs
ist **Accepted**, andere noch **Proposed** (siehe [Index](docs/decisions/README.md)).

## Installation (1-Zeiler)

Auf einem Linux-Host (x86_64 oder aarch64) mit öffentlichem DNS-A-Record ([ADR-0027](docs/decisions/0027-d4y-host-bundle-systemd.md)):

```bash
D4Y_HOST=d4y.example.com D4Y_ACME_EMAIL=you@example.com \
  sh -c "$(curl -fsSL https://grundner.github.io/d4y/install.sh)"
```

Der Installer lädt das d4y-**Bundle** (App + eingebettetes JRE, **kein System-Java**) vom
GitHub-Release, entpackt es nach `/opt/d4y` und richtet einen **systemd-Service** ein. Docker wird bei
Bedarf installiert — d4y läuft direkt auf dem Host, orchestriert Traefik/Apps über den Docker-Socket
und stellt seine eigene HTTPS-Route (ACME) per Traefik-File-Provider her. d4y hält **keine**
GitHub-Credentials — Sollzustand und Secrets liefert ein GitHub-Actions-Workflow im Config-Repo per
Push ([ADR-0025](docs/decisions/0025-full-push-desired-state-delivery.md), Vorlage:
[`site/config-repo-workflow.yml`](site/config-repo-workflow.yml)).

## Betrieb

```bash
systemctl status d4y       # Zustand
journalctl -u d4y -f       # Logs
systemctl restart d4y      # Neustart
```

## Build & Release

```bash
./gradlew build            # Fat-Jar (Backend + eingebettetes Frontend); -PskipFrontend überspringt das Frontend
./gradlew bootRun          # lokal starten → http://localhost:8080
./gradlew bundleTar        # Host-Bundle build/dist/d4y-<version>.tar.gz (jlink + jpackage)
```

Versionierung und Auslieferung sind in
[`docs/architecture/release-and-versioning.md`](docs/architecture/release-and-versioning.md)
beschrieben ([ADR-0027](docs/decisions/0027-d4y-host-bundle-systemd.md)): Git-Tag `vX.Y.Z` als
Versions-Wahrheit; bei einem Tag baut die CI das Bundle und hängt es als Asset an das GitHub-Release.

## Dokumentation

Die maßgeblichen Quellen und die Arbeitsweise definiert [`CLAUDE.md`](CLAUDE.md). Einstieg:

- **Vision:** [`docs/product/vision.md`](docs/product/vision.md) *(sekundär, kein Implementierungstreiber)*
- **Architektur:** [`docs/architecture/overview.md`](docs/architecture/overview.md)
- **Domänenmodell:** [`docs/domain/`](docs/domain/)
- **Entscheidungen (ADRs):** [`docs/decisions/README.md`](docs/decisions/README.md)
- **Regeln:** [`docs/rules/`](docs/rules/)
- **Standards:** [`docs/standards/`](docs/standards/)
- **UI:** [`docs/ui/status-view.md`](docs/ui/status-view.md)

## Arbeitsweise

Dieses Projekt ist **dokumentationsgetrieben**: Änderungen beginnen in der Dokumentation.
Nur **Accepted** ADRs und Domänendokumente treiben die Implementierung. Details:
[`docs/standards/documentation.md`](docs/standards/documentation.md).

## Geplante Technik (siehe ADRs)

- **Backend:** Java 21 + Spring Boot — [ADR-0003](docs/decisions/0003-java21-spring-boot-backend.md)
- **Frontend:** Next.js + React, read-only — [ADR-0004](docs/decisions/0004-nextjs-react-readonly-frontend.md)
- **Container-Backend:** Abstraktion, Docker zuerst — [ADR-0005](docs/decisions/0005-container-backend-abstraction-docker-first.md)
- **Auslieferung:** d4y als Host-Bundle (jlink) unter systemd — [ADR-0027](docs/decisions/0027-d4y-host-bundle-systemd.md) *(löst das Single-Image aus [ADR-0006](docs/decisions/0006-single-container-image-backend-frontend.md) ab)*
