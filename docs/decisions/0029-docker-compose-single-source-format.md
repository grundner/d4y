# ADR-0029: Docker Compose als einziges Quellformat, ausgeführt über die `docker compose`-CLI

Status: Proposed
Datum: 2026-07-13
Betrifft: [ADR-0002](0002-immutable-images-no-build-on-target.md), [ADR-0005](0005-container-backend-abstraction-docker-first.md), [ADR-0007](0007-continuous-reconciliation-self-healing.md), [ADR-0009](0009-persistence-optional-backup-restore.md), [ADR-0016](0016-reverse-proxy-traefik-docker-labels.md), [ADR-0018](0018-service-discovery-and-dns-mode.md), [ADR-0019](0019-git-config-repository-source.md), [ADR-0020](0020-backup-restore-s3-rclone.md), [ADR-0024](0024-delivered-image-secrets-encrypted-store.md), [ADR-0025](0025-full-push-desired-state-delivery.md), [ADR-0028](0028-per-route-tls-and-http-mode.md), [architecture/desired-state-yaml](../architecture/desired-state-yaml.md)

## Kontext

Bisher beschreibt der Sollzustand jede App in einem **proprietären YAML** (`name`, `image`,
`volumes`, `routes`, `env`, `backup`) mit **genau einem Container pro App**; d4y erstellt Container
direkt über die Docker-Engine-API (Socket) und rendert Routes selbst in Traefik-Labels.

Das Ziel der Plattform ist, Apps **schnell und reproduzierbar** auf Servern hochzufahren. In der
Praxis liegen App-Definitionen fast immer schon als **Docker Compose** vor: vertraut, verbreitet,
lokal mit `docker compose up` lauffähig, multi-service-fähig, und mit `build:` auch für lokal gebaute
Images. Das proprietäre Format zwingt zu Doppelpflege und kann Multi-Service-Apps nicht abbilden.

Zwei harte technische Randbedingungen:
- Über die **Docker-Socket-API** lässt sich **kein** Compose ausführen (kein `build`, kein
  Multi-Service-Graph, kein `depends_on`/Healthcheck-Handling). Es gibt keine brauchbare
  JVM-Compose-Implementierung.
- Compose ist das kanonische Format der `docker compose`-CLI.

## Entscheidung

1. **Docker Compose ist das einzige Quellformat.** Der Sollzustand besteht aus **einem Verzeichnis
   pro App**; darin eine **standardkonforme, valide** `compose.yaml` (bzw. `docker-compose.yml`) plus
   optionale Begleitdateien (`Dockerfile`, `.env`, `env_file`, Config-Dateien). Die `compose.yaml`
   bleibt herstellerneutral und ist lokal mit `docker compose up` lauffähig.

2. **Bauen auf dem Ziel ist erlaubt.** `build:` in Compose ist zulässig — dies **löst
   [ADR-0002](0002-immutable-images-no-build-on-target.md) ab** (kein Build auf dem Zielsystem).
   Vorgebaute Images aus Registries bleiben gleichwertig möglich und für schwere/binäre Build-Kontexte
   weiterhin empfohlen.

3. **Ausführung über die `docker compose`-CLI** (kein direkter Engine-API-Container-Create mehr). Der
   Reconcile-Loop ([ADR-0007](0007-continuous-reconciliation-self-healing.md)) bleibt; sein Executor
   wird pro App: `docker compose -p d4y-<app> -f compose.yaml -f <d4y-override> up -d --remove-orphans`.
   Build nur bei Änderung (Push-getriggert `--build`; periodischer Self-Heal-Heartbeat ohne `--build`).
   Voraussetzung: das `docker compose`-Plugin auf dem Host (der Installer stellt es sicher).

4. **d4y-Sidecar `d4y.yaml` je App-Verzeichnis** für d4y-spezifische Belange, damit `compose.yaml`
   sauber bleibt. Enthält **Routes/Ingress** und **Backup-Opt-in**. Aus `compose.yaml` (Service-Namen)
   + `d4y.yaml` generiert d4y ein **Override-Compose**, das (a) alle Services ans externe `d4y`-Netz
   hängt und (b) am Ziel-Service die Traefik-Labels der Routes injiziert. Die TLS-Logik aus
   [ADR-0028](0028-per-route-tls-and-http-mode.md) (pro Route `tls`, globaler Default aus ACME) bleibt
   erhalten, wandert aber ins Sidecar/den Override. Fehlt `d4y.yaml`, läuft die App ohne externen
   Ingress (nur intern) und ohne Backup.

5. **Ownership/Drift über Compose-Projekte.** Verwaltete Projekte tragen
   `com.docker.compose.project=d4y-<app>`. Ein Projekt ohne Deklaration → `docker compose down`
   (ersetzt das bisherige „StopAndRemove undeklarierter Container"). Einheit ist das **Compose-Projekt**
   (ggf. mehrere Services/Container), nicht mehr ein einzelner Container.

6. **Secrets als Prozess-Env.** Der verschlüsselte, per Push gelieferte Secret-Store
   ([ADR-0024](0024-delivered-image-secrets-encrypted-store.md)) bleibt. d4y löst Secrets auf und
   übergibt sie als **Umgebungsvariablen an den `docker compose`-Prozess**; Compose interpoliert sie
   nativ (`${VAR}`/`env_file`). Secrets werden **nie im Klartext auf Platte** gerendert. Die
   proprietäre `${secret:NAME}`-Syntax entfällt.

7. **Ingress/Netz/Backup.** Der von d4y gemanagte **Traefik** und das **`d4y`-Netz**
   ([ADR-0016](0016-reverse-proxy-traefik-docker-labels.md)) bleiben — das Netz nun als **external**
   Netz, dem Compose-Services über den Override beitreten. Backup ([ADR-0020](0020-backup-restore-s3-rclone.md))
   bleibt (rclone→S3), Discovery erfolgt über die Compose-Volume-Namen + das `backup`-Opt-in im Sidecar.

Diese ADR **löst [ADR-0002](0002-immutable-images-no-build-on-target.md) ab** und **ergänzt/ändert**
ADR-0009, ADR-0016, ADR-0018, ADR-0019, ADR-0020, ADR-0024, ADR-0025 und ADR-0028 entsprechend.

## Konsequenzen

- **Positiv:** Vertrautes Standardformat; App-Definitionen sind lokal lauffähig; **Multi-Service-Apps**
  werden nativ möglich; Bauen auf dem Ziel erlaubt; d4y muss kein eigenes Format pflegen.
- **Positiv:** `compose.yaml` bleibt herstellerneutral (d4y-Spezifika im Sidecar).
- **Negativ / Charakterwechsel:** d4y verschiebt sich vom **eigenen** Container-Reconciler zu einer
  **Compose-Orchestrierungs-Schicht** (+ Traefik, Backup, Git/Push, Self-Healing). Die Vision-Aussage
  „nicht ein weiterer Orchestrator" ist entsprechend zu präzisieren: d4y orchestriert nun **via
  Compose**, bleibt aber die **Git-native, selbstheilende Runtime-Schicht**.
- **Negativ (ADR-0005-Spannung):** Compose/`docker compose` ist Docker-spezifisch. Der Executor lebt
  daher als **Adapter** hinter einem Port; die engine-neutrale Kernlogik bleibt so weit wie möglich
  gewahrt, aber das Quellformat ist nun Docker-nah. Bewusst akzeptiert.
- **Negativ:** Neue Laufzeitabhängigkeit `docker compose`-Plugin; Reconcile wird gröber (Projekt statt
  Container); Push-Contract muss Verzeichnisbäume tragen (statt flacher Dateien).
- **Sicherheit:** `build:` bedeutet Code-Ausführung auf dem Host beim Bauen — bewusst erlaubt; große/
  binäre Build-Kontexte besser vorbauen und als Image beziehen.

## Alternativen

- **Compose parsen und selbst über die Engine-API ausführen** — verworfen: reimplementiert Compose
  (Build, `depends_on`, Healthchecks) in Java; unrealistisch.
- **Compose nur als optionaler Import, proprietäres Format als Kern** — vom User verworfen zugunsten
  „Compose als einziges Format".
- **d4y-Spezifika direkt als `x-d4y`/Traefik-Labels in die `compose.yaml`** — verworfen zugunsten der
  Sidecar-Datei, damit die `compose.yaml` neutral/lokal-lauffähig bleibt.
- **`${secret:}` in die Compose-Dateien rendern** — verworfen: schriebe Klartext-Secrets auf Platte;
  stattdessen Prozess-Env.
