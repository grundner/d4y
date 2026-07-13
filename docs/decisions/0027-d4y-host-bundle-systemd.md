# ADR-0027: d4y als selbst-enthaltendes Host-Bundle (jlink) unter systemd statt Container

Status: Proposed
Datum: 2026-07-13
Betrifft: [ADR-0006](0006-single-container-image-backend-frontend.md), [ADR-0022](0022-release-versioning-image-pipeline.md), [ADR-0026](0026-one-liner-bootstrap-github.md), [ADR-0016](0016-reverse-proxy-traefik-docker-labels.md), [ADR-0017](0017-tls-https-ingress.md), [ADR-0008](0008-bootstrap-single-command-install.md), [architecture/bootstrap](../architecture/bootstrap.md), [architecture/release-and-versioning](../architecture/release-and-versioning.md)

## Kontext

Bisher wird **d4y selbst** als Container ausgeliefert: ein OCI-Image auf GHCR ([ADR-0022](0022-release-versioning-image-pipeline.md),
[ADR-0006](0006-single-container-image-backend-frontend.md)), gestartet vom 1-Zeiler-Installer per
`docker run --restart unless-stopped` ([ADR-0026](0026-one-liner-bootstrap-github.md)).

d4y soll den **Host selbst auf Gesundheit und Konsistenz überwachen** (nicht nur die von d4y
verwalteten Container). Aus einem Container heraus ist der direkte Host-Zugriff eingeschränkt und
unnatürlich. d4y soll daher **direkt auf dem Host** laufen. Zugleich soll **kein System-Java**
installiert werden müssen. Ein reines Fat-JAR erfüllt das nicht — es bringt keine JVM mit und
benötigt ein installiertes `java`.

Wichtig: d4y bleibt ein **Docker-Orchestrator** — es verwaltet Traefik und alle App-Container über
`/var/run/docker.sock`. Der Wegfall des d4y-Containers entfernt die Docker-Abhängigkeit also **nicht**;
Docker bleibt auf dem Host Pflicht.

## Entscheidung

1. **d4y läuft direkt auf dem Host als selbst-enthaltendes Bundle.** Der Build erzeugt per
   **jlink + jpackage** ein `app-image`: die Anwendung (Fat-JAR inkl. eingebettetem Frontend, ADR-0006
   bleibt inhaltlich gewahrt) plus ein **eingebettetes Minimal-JRE**. Kein System-Java, kein Container
   für d4y selbst. Ausgeliefert als Tarball (`d4y-<version>-linux-x86_64.tar.gz`).

2. **Docker bleibt Voraussetzung.** d4y orchestriert Traefik und Apps unverändert über den
   Docker-Socket. Der Installer stellt Docker weiterhin bei Bedarf sicher.

3. **systemd besitzt den Lifecycle.** Ein Unit `d4y.service` (`Type=simple`, `Restart=always`,
   `Requires=docker.service`, `After=docker.service network-online.target`) startet d4y beim Boot und
   startet es bei Absturz neu. Damit entfällt die bisherige Boot-Absicherung über Docks
   `--restart unless-stopped` — für **d4y selbst**; verwaltete Container behalten ihre Restart-Policy.

4. **Selbst-Ingress via Traefik-File-Provider.** Da d4y kein Container mehr ist, kann Traefiks
   Docker-Provider d4ys eigene Route nicht mehr aus Container-Labels lesen. d4y schreibt seine eigene
   Route deshalb **im Code** in eine dynamische Traefik-Config-Datei (File-Provider), Ziel
   `http://host.docker.internal:8080` (Host-Gateway). Der Traefik-Container erhält dafür
   `--providers.file.directory=/dynamic` und `--add-host host.docker.internal:host-gateway`.
   **Verwaltete App-Container bleiben beim Docker-Provider** ([ADR-0016](0016-reverse-proxy-traefik-docker-labels.md)) —
   der File-Provider gilt ausschließlich für d4ys eigene Route.

5. **Auslieferung als GitHub-Release-Asset.** Die CI baut bei Tag `vX.Y.Z` das Bundle und hängt das
   Tarball an das GitHub-Release. Der Installer lädt `…/releases/latest/download/…`, entpackt nach
   `/opt/d4y`, legt Persistenz unter `/var/lib/d4y` an und richtet den systemd-Service ein. **Das
   GHCR-Image entfällt** ([ADR-0022](0022-release-versioning-image-pipeline.md) abgelöst).

Diese ADR **löst** [ADR-0006](0006-single-container-image-backend-frontend.md),
[ADR-0022](0022-release-versioning-image-pipeline.md) und [ADR-0026](0026-one-liner-bootstrap-github.md)
**ab** und **ergänzt** [ADR-0016](0016-reverse-proxy-traefik-docker-labels.md) sowie
[ADR-0017](0017-tls-https-ingress.md) um die File-Provider-Selbstroute.

## Konsequenzen

- **Positiv:** d4y kann den Host direkt überwachen; kein System-Java nötig (JRE ist eingebettet);
  sauberer Betrieb über `systemctl status/restart/stop d4y` und Logs via `journalctl -u d4y`.
- **Positiv:** Ein Auslieferungsweg (Bundle) statt Image + Registry-Sichtbarkeitspflege; kein
  `unauthorized`-Risiko wie beim anonymen GHCR-Pull.
- **Negativ:** Der Build wird komplexer (jlink-Modul-Set via `jdeps` pinnen, jpackage-Schritt) und ist
  plattformgebunden (Linux/x86_64-Runner für das Linux-Bundle).
- **Negativ:** d4y hört auf dem Host auf `:8080` — erreichbar für Traefik via Host-Gateway, aber ohne
  Härtung auch lokal offen. **Härtung:** an die Docker-Bridge-Gateway-IP binden oder `:8080` per
  Firewall auf lokal beschränken.
- **Negativ:** `host.docker.internal:host-gateway` benötigt Docker ≥ 20.10 (auf modernen Debian/Ubuntu
  gegeben).
- **Negativ:** Die Route von d4y liegt nun in einer Datei außerhalb der Container-Labels; sie wird von
  d4y idempotent (Start + Reconcile) neu geschrieben und bleibt so selbstheilend.

## Alternativen

- **GraalVM Native Image** (eine Binary ohne jede JVM) — attraktiv für Footprint/Startzeit, aber
  reflection-lastige Abhängigkeiten (Netty-Docker-Client, Spring, JGit, Jackson) erfordern Native-Hints
  und mehr Build-Risiko. Für diesen Schnitt zugunsten des kompatibilitätssicheren jlink-Bundles
  verworfen; als spätere Option offen.
- **Fat-JAR + System-JRE installieren** — verworfen: widerspricht dem Ziel „kein System-Java".
- **d4y im Container mit Host-Mounts** (`/proc`, `/`, `/sys`) fürs Host-Monitoring — technisch möglich,
  aber umständlicher und weniger direkt als der Host-Betrieb; verworfen zugunsten der Host-Variante.
- **Traefik File-Provider auch für App-Routen** — verworfen: ADR-0016 hält am Docker-Provider für
  Container fest; der File-Provider wird nur für d4ys eigene, container-lose Route genutzt.
</content>
