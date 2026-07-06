# Architecture Decision Records (ADRs)

Dieser Ordner enthält alle Architektur- und Domänenentscheidungen als ADRs. Nur ADRs mit
**Status: Accepted** sind bindend. Der ADR-Prozess ist in
[../standards/adr.md](../standards/adr.md) beschrieben; die Vorlage in
[0000-adr-template.md](0000-adr-template.md).

## Index

| ADR | Titel | Status |
| --- | --- | --- |
| [0001](0001-git-as-single-source-of-truth.md) | Git als einzige Quelle der Wahrheit | Accepted |
| [0002](0002-immutable-images-no-build-on-target.md) | Unveränderliche Images, kein Build auf dem Zielsystem | Proposed |
| [0003](0003-java21-spring-boot-backend.md) | Backend auf Basis von Java 21 und Spring Boot | Accepted |
| [0004](0004-nextjs-react-readonly-frontend.md) | Frontend Next.js/React — Sollzustand read-only, operative Aktionen erlaubt | Proposed |
| [0005](0005-container-backend-abstraction-docker-first.md) | Container-Backend-Abstraktion, Docker zuerst | Accepted |
| [0006](0006-single-container-image-backend-frontend.md) | Backend + Frontend als einzelnes Image | Accepted |
| [0007](0007-continuous-reconciliation-self-healing.md) | Kontinuierliche Reconciliation und Self-Healing | Accepted |
| [0008](0008-bootstrap-single-command-install.md) | Inbetriebnahme über einen Bootstrap-Befehl | Proposed |
| [0009](0009-persistence-optional-backup-restore.md) | Persistenz über optionales Backup/Restore | Proposed |
| [0010](0010-dns-ingress-service-discovery.md) | DNS, Ingress und Service-Discovery | Proposed |
| [0011](0011-interim-local-desired-state-source.md) | Interim: lokale Desired-State-Quelle vor Git-Anbindung | Proposed |
| [0012](0012-operational-actions-and-reconciliation-hold.md) | Operative Aktionen und Reconciliation-Hold | Proposed |

> **Accepted** ADRs sind bindend. **Proposed** ADRs sind Entwürfe und werden erst nach
> expliziter Freigabe auf **Accepted** gesetzt (siehe [../standards/adr.md](../standards/adr.md)).
