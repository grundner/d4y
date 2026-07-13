# Architecture Decision Records (ADRs)

Dieser Ordner enthält alle Architektur- und Domänenentscheidungen als ADRs. Nur ADRs mit
**Status: Accepted** sind bindend. Der ADR-Prozess ist in
[../standards/adr.md](../standards/adr.md) beschrieben; die Vorlage in
[0000-adr-template.md](0000-adr-template.md).

## Index

| ADR | Titel | Status |
| --- | --- | --- |
| [0001](0001-git-as-single-source-of-truth.md) | Git als einzige Quelle der Wahrheit | Accepted |
| [0002](0002-immutable-images-no-build-on-target.md) | Unveränderliche Images, kein Build auf dem Zielsystem | Superseded by 0029 |
| [0003](0003-java21-spring-boot-backend.md) | Backend auf Basis von Java 21 und Spring Boot | Accepted |
| [0004](0004-nextjs-react-readonly-frontend.md) | Frontend Next.js/React — Sollzustand read-only, operative Aktionen erlaubt | Proposed |
| [0005](0005-container-backend-abstraction-docker-first.md) | Container-Backend-Abstraktion, Docker zuerst | Accepted |
| [0006](0006-single-container-image-backend-frontend.md) | Backend + Frontend als einzelnes Image | Superseded by 0027 |
| [0007](0007-continuous-reconciliation-self-healing.md) | Kontinuierliche Reconciliation und Self-Healing | Accepted |
| [0008](0008-bootstrap-single-command-install.md) | Inbetriebnahme über einen Bootstrap-Befehl | Proposed |
| [0009](0009-persistence-optional-backup-restore.md) | Persistenz über optionales Backup/Restore | Proposed |
| [0010](0010-dns-ingress-service-discovery.md) | DNS, Ingress und Service-Discovery | Proposed |
| [0011](0011-interim-local-desired-state-source.md) | Interim: lokale Desired-State-Quelle vor Git-Anbindung | Proposed |
| [0012](0012-operational-actions-and-reconciliation-hold.md) | Operative Aktionen und Reconciliation-Hold | Accepted |
| [0013](0013-operational-actions-and-hold-api.md) | Operative-Aktionen- und Hold-API | Accepted |
| [0014](0014-frontend-static-export-served-by-backend.md) | Frontend als statischer Export, vom Backend ausgeliefert | Accepted |
| [0015](0015-frontend-dark-blue-theme.md) | Frontend Dark-Theme Blau/Lila | Accepted |
| [0016](0016-reverse-proxy-traefik-docker-labels.md) | Reverse-Proxy via Traefik (Docker-Label-Provider) | Accepted |
| [0017](0017-tls-https-ingress.md) | TLS/HTTPS für Ingress | Accepted |
| [0018](0018-service-discovery-and-dns-mode.md) | Interne Service-Discovery und DNS-Modus | Accepted |
| [0019](0019-git-config-repository-source.md) | Sollzustand aus Git-Config-Repository (JGit, HTTPS) | Accepted |
| [0020](0020-backup-restore-s3-rclone.md) | Backup/Restore von Volumes in einen S3-Backup-Store (rclone) | Accepted |
| [0021](0021-published-api-contract-openapi.md) | Publizierter, generierter API-Vertrag (OpenAPI) | Proposed |
| [0022](0022-release-versioning-image-pipeline.md) | Release-Versionierung und Image-Pipeline (SemVer/Git-Tag, Buildpacks, GHCR) | Superseded by 0027 |
| [0023](0023-push-triggered-reconcile-and-trigger-auth.md) | Push-getriggertes Reconcile und authentifizierter Trigger-Endpoint | Proposed |
| [0024](0024-delivered-image-secrets-encrypted-store.md) | Gelieferte Image/Container-Secrets und verschlüsselter lokaler Secret-Store | Proposed |
| [0025](0025-full-push-desired-state-delivery.md) | Voll-Push-Auslieferung des Sollzustands | Proposed |
| [0026](0026-one-liner-bootstrap-github.md) | 1-Zeiler-Bootstrap und Verteilung über GitHub | Superseded by 0027 |
| [0027](0027-d4y-host-bundle-systemd.md) | d4y als selbst-enthaltendes Host-Bundle (jlink) unter systemd | Proposed |
| [0028](0028-per-route-tls-and-http-mode.md) | Pro-Route-TLS und HTTP-Betrieb ohne öffentliche IP | Proposed |
| [0029](0029-docker-compose-single-source-format.md) | Docker Compose als einziges Quellformat (`docker compose`-CLI) | Proposed |

> **Accepted** ADRs sind bindend. **Proposed** ADRs sind Entwürfe und werden erst nach
> expliziter Freigabe auf **Accepted** gesetzt (siehe [../standards/adr.md](../standards/adr.md)).
