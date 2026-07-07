# Standard — Coding

> **Status: In Umsetzung.** Verbindliche technische Festlegungen erfolgen über **Accepted** ADRs.
> Der erste Backend-Schnitt (Reconciliation-Loop) ist implementiert.

## Backend (Java 21 / Spring Boot)

Bezug: [ADR-0003](../decisions/0003-java21-spring-boot-backend.md)

- **Sprachlevel:** Java 21; moderne Features (Records, Sealed Types, Pattern Matching)
  bevorzugt, wo sie die Lesbarkeit erhöhen.
- **Build:** Gradle (Kotlin DSL) im Repo-Root; Java-Toolchain auf 21 gepinnt.
- **Architektur (Ports & Adapter):** Kernlogik bleibt **frei von Engine- und Framework-Spezifika**.
  → [container-backend-abstraction](../architecture/container-backend-abstraction.md)
  - `io.d4y.domain` — reines Domänenmodell + `Reconciler` (seiteneffekt- und framework-frei).
  - `io.d4y.port` — Ports (`ContainerBackend`, `DesiredStateSource`).
  - `io.d4y.adapter` — Adapter (Docker über HTTP/Unix-Socket; YAML-Desired-State).
  - `io.d4y.app` — Orchestrierung (`ReconciliationLoop`).
  - `io.d4y.api` — read-only Status-API.
- **Determinismus:** keine impliziten Umgebungsabhängigkeiten in der Kernlogik.
- **Tests:** die Reconciler-Diff-Logik wird ohne echte Engine getestet (Fake-Backend); fachliche
  Regeln aus `docs/domain` sind durch Tests abzusichern.

### Umsetzungsstand
- Reconciliation-Loop für **Applications** (start/stop/replace, Drift-Bereinigung, Self-Healing).
- Desired State aus lokaler YAML — **Interim** [ADR-0011](../decisions/0011-interim-local-desired-state-source.md),
  Git-Anbindung folgt.
- **Operative Aktionen & Hold** ([ADR-0013](../decisions/0013-operational-actions-and-hold-api.md)):
  Restart/Stop/Logs/Details/exec/temp. Parameter, In-Memory-Hold (Reconciler-Skip, auto-expiring)
  und In-Memory-Audit; `GET /api/status` um Hold-Infos erweitert. Akteur via `X-Actor`.
- Noch nicht umgesetzt: Volumes/Backup, Routes/DNS (Backend), Single-Image, Bootstrap, Auth,
  Secret-Masking; Frontend-Verdrahtung dieser Endpunkte.

## Frontend (Next.js / React)

Bezug: [ADR-0004](../decisions/0004-nextjs-react-readonly-frontend.md)

- **Read-only:** die UI löst keine Infrastrukturänderungen aus.
- **Zweck:** Statuskontrolle und Visualisierung des Plattformzustands.
  → [status-view](../ui/status-view.md)

## Allgemein

- Code darf der Dokumentation nicht widersprechen ([documentation](documentation.md)).
- Verhaltensänderungen ziehen Doku-Aktualisierungen nach sich.
