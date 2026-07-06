# Architecture Decision Records (ADRs)

Dieser Ordner enthält alle Architektur- und Domänenentscheidungen als ADRs. Nur ADRs mit
**Status: Accepted** sind bindend. Der ADR-Prozess ist in
[../standards/adr.md](../standards/adr.md) beschrieben; die Vorlage in
[0000-adr-template.md](0000-adr-template.md).

## Index

| ADR | Titel | Status |
| --- | --- | --- |
| [0001](0001-git-as-single-source-of-truth.md) | Git als einzige Quelle der Wahrheit | Proposed |
| [0002](0002-immutable-images-no-build-on-target.md) | Unveränderliche Images, kein Build auf dem Zielsystem | Proposed |
| [0003](0003-java21-spring-boot-backend.md) | Backend auf Basis von Java 21 und Spring Boot | Proposed |
| [0004](0004-nextjs-react-readonly-frontend.md) | Frontend Next.js/React, zunächst read-only | Proposed |
| [0005](0005-container-backend-abstraction-docker-first.md) | Container-Backend-Abstraktion, Docker zuerst | Proposed |
| [0006](0006-single-container-image-backend-frontend.md) | Backend + Frontend als einzelnes Image | Proposed |
| [0007](0007-continuous-reconciliation-self-healing.md) | Kontinuierliche Reconciliation und Self-Healing | Proposed |
| [0008](0008-bootstrap-single-command-install.md) | Inbetriebnahme über einen Bootstrap-Befehl | Proposed |
| [0009](0009-persistence-optional-backup-restore.md) | Persistenz über optionales Backup/Restore | Proposed |

> Alle ADRs sind aktuell **Proposed** und damit noch nicht bindend. Sie werden erst nach
> expliziter Freigabe auf **Accepted** gesetzt.
