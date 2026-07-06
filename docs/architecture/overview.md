# Architektur — Überblick

Status: Draft

D4Y ist eine schlanke **Runtime-Schicht** über einer Container-Engine. Sie liest den
Sollzustand aus einem Git-Konfigurationsrepository, gleicht ihn kontinuierlich mit dem
tatsächlichen Zustand der Maschine ab und stellt den Sollzustand ohne manuelle Eingriffe her.

Verbindliche Grundlagen sind die **Accepted** ADRs in [`../decisions/`](../decisions/) sowie
die Domänenmodelle in [`../domain/`](../domain/). Solange ADRs den Status `Proposed` tragen,
sind sie noch nicht bindend.

## Komponenten

```text
        ┌────────────────────────┐
        │  Git-Config-Repository │  (Sollzustand, einzige Quelle der Wahrheit)
        └───────────┬────────────┘
                    │ pull
                    ▼
 ┌───────────────────────────────────────────┐
 │                 D4Y-Runtime                │  (ein Container-Image)
 │                                            │
 │  ┌───────────────┐   ┌──────────────────┐  │
 │  │ Backend       │   │ Frontend         │  │
 │  │ Java 21 /     │◀──│ Next.js / React  │  │
 │  │ Spring Boot   │   │ (read-only UI)   │  │
 │  └──────┬────────┘   └──────────────────┘  │
 │         │ Reconciliation-Loop              │
 │         ▼                                  │
 │  ┌────────────────────────────────────┐   │
 │  │ Container-Backend-Abstraktion      │   │
 │  └──────────────┬─────────────────────┘   │
 └─────────────────┼─────────────────────────┘
                   ▼
        ┌────────────────────────┐
        │ Container-Engine (Docker) │  (erste Implementierung)
        └────────────────────────┘
                   ▲
                   │ pull (unveränderliche Images)
        ┌────────────────────────┐
        │ Vertrauenswürdige Registry │  (Quelle für Code / Images)
        └────────────────────────┘

        ┌────────────────────────┐
        │ Backup-Store            │  (Quelle/Ziel für Daten — Backup & Restore)
        └────────────────────────┘
             ▲ backup / ▼ restore (nur bei leerem Volume)
        gesteuert durch das Backend
```

Registry und Backup-Store sind symmetrisch: **Code** ist über Images aus der Registry
reproduzierbar, **Daten** über Backups aus dem Backup-Store.
→ [persistence-and-backup](persistence-and-backup.md)

## Schichten

| Schicht | Verantwortung | Referenz |
| --- | --- | --- |
| Git-Config-Repo | Deklarativer Sollzustand | [config-repository](../domain/config-repository.md), [ADR-0001](../decisions/0001-git-as-single-source-of-truth.md) |
| Backend | Reconciliation, Zustandsverwaltung, API | [ADR-0003](../decisions/0003-java21-spring-boot-backend.md) |
| Frontend | Read-only Status & Visualisierung | [ADR-0004](../decisions/0004-nextjs-react-readonly-frontend.md), [status-view](../ui/status-view.md) |
| Container-Backend-Abstraktion | Engine-neutrale Steuerung | [container-backend-abstraction](container-backend-abstraction.md), [ADR-0005](../decisions/0005-container-backend-abstraction-docker-first.md) |
| Container-Engine | Ausführung der Container | Docker (erste Impl.) |
| Backup-Store | Externes Ziel/Quelle für Datendurability | [backup-store](../domain/backup-store.md), [persistence-and-backup](persistence-and-backup.md), [ADR-0009](../decisions/0009-persistence-optional-backup-restore.md) |

## Betriebsmodell

- **Änderungen erfolgen ausschließlich über das Git-Config-Repo.** Das Frontend und die API
  sind für Infrastrukturänderungen bewusst read-only. → [business-rules](../rules/business-rules.md)
- **Server sind austauschbar und zustandslos.** Jeglicher Zustand ergibt sich aus Git plus dem
  laufenden Ist-Zustand der Engine. → [server](../domain/server.md)
- **Auslieferung als Single-Image.** Backend und Frontend liegen in der ersten Ausbaustufe in
  einem Image. → [ADR-0006](../decisions/0006-single-container-image-backend-frontend.md)

## Vertiefende Dokumente

- [container-backend-abstraction.md](container-backend-abstraction.md)
- [reconciliation-loop.md](reconciliation-loop.md)
- [bootstrap.md](bootstrap.md)
- [persistence-and-backup.md](persistence-and-backup.md)
