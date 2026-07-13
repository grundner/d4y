# ADR-0026: 1-Zeiler-Bootstrap und Verteilung über GitHub

Status: Proposed
Datum: 2026-07-13
Betrifft: [ADR-0008](0008-bootstrap-single-command-install.md), [ADR-0022](0022-release-versioning-image-pipeline.md), [ADR-0025](0025-full-push-desired-state-delivery.md), [ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md), [ADR-0017](0017-tls-https-ingress.md), [ADR-0016](0016-reverse-proxy-traefik-docker-labels.md)

## Kontext

[ADR-0008](0008-bootstrap-single-command-install.md) sieht eine Ein-Befehl-Inbetriebnahme vor, die
„Config-Repo + Credentials" abfragt — ist aber konzeptionell und regelt Transportmittel/Ports/Volumes
noch nicht. Mit der Voll-Push-Architektur ([ADR-0025](0025-full-push-desired-state-delivery.md))
entfällt die Config-Repo-Credential-Prämisse: d4y hält **keine** GitHub-Credentials mehr.

Ziel: eine **1-Zeiler-Installation**, bei der **alle Artefakte auf GitHub** liegen (Image → GHCR,
Website + Installer → GitHub Pages).

## Entscheidung

1. **Image aus öffentlichem GHCR.** `ghcr.io/grundner/d4y:latest`
   ([ADR-0022](0022-release-versioning-image-pipeline.md)) wird **public** — der Pull ist **anonym**,
   ohne Credential.
2. **Installer auf GitHub Pages.** `curl -fsSL https://grundner.github.io/d4y/install.sh | sh`. Der
   Installer und die Projekt-Website werden per GitHub Pages aus dem d4y-Repo ausgeliefert.
3. **Self-generierte Credentials.** Der Installer erzeugt lokal ein zufälliges **Trigger-Token**
   ([ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md)) und einen **Encryption-Key**
   ([ADR-0024](0024-delivered-image-secrets-encrypted-store.md)) — host/d4y-Credentials ohne
   GitHub-Bezug. Sie werden dem Container als Env übergeben.
4. **Container-Betrieb.** `docker run` mit: Docker-Socket-Mount (`/var/run/docker.sock`), Named Volume
   für Persistenz (Desired-Verzeichnis, Secret-Store, ACME), am `d4y`-Netz
   ([ADR-0016](0016-reverse-proxy-traefik-docker-labels.md)), `--restart unless-stopped`. Interner
   Port 8080 wird **nicht** direkt publiziert.
5. **Erreichbarkeit via öffentliche IP/DNS + Traefik/ACME.** d4ys eigener Endpoint wird über den von
   d4y gemanagten Traefik ([ADR-0017](0017-tls-https-ingress.md)) exponiert: der Installer setzt am
   d4y-Container die Traefik-Labels (Router `Host(<host>)`, `entrypoints=websecure`, `tls.certresolver=le`,
   Service-Port 8080). ACME (HTTP-01) über den öffentlichen A-Record; `D4Y_INGRESS_TLS_ACME_EMAIL` gesetzt.
6. **Verdrahtung mit GitHub.** Der Installer gibt `D4Y_URL=https://<host>` und das Trigger-Token aus;
   der Nutzer hinterlegt beide **einmalig** als GitHub-Actions-Secrets im Config-Repo. Ab da pusht die
   Actions-Pipeline Config+Secrets ([ADR-0025](0025-full-push-desired-state-delivery.md)).

## Konsequenzen

- **Positiv:** Echte 1-Zeiler-Installation, alle Artefakte auf GitHub; keine GitHub-Credentials in d4y.
- **Positiv:** TLS/ACME bereits über den vorhandenen Traefik-Weg abgedeckt.
- **Negativ:** `curl … | sh` ist ein Vertrauens-/Integritätsrisiko — Prüfsumme veröffentlichen; eine
  transparente `docker run`-Variante als Alternative dokumentieren.
- **Negativ:** d4y muss öffentlich erreichbar sein; das öffentliche GHCR-Image gibt die Image-Inhalte
  preis (aber keine Secrets).
- **Negativ:** Das Buildpack-Image läuft non-root — Volume-Berechtigungen für die Persistenzpfade sind
  im Installer zu berücksichtigen.

Diese ADR **konkretisiert** [ADR-0008](0008-bootstrap-single-command-install.md): der Bootstrap fragt
**Host + ACME-Mail** statt Config-Repo-Credentials.

## Alternativen

- **Tunnel (cloudflared o. ä.)** statt öffentlicher IP — gültige Alternative für NAT/Heimumgebungen;
  hier wurde öffentliche IP/DNS gewählt. Als spätere Option offen.
- **d4y-Selbstregistrierung per Traefik-File-Provider** statt Labels am `docker run` — sauberer, aber
  Code-Aufwand; Follow-up.
- **Privates GHCR-Image mit Pull-Secret** — verworfen: widerspräche dem credential-freien 1-Zeiler.
