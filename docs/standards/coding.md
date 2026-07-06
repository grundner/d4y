# Standard — Coding (Platzhalter)

> **Status: Draft.** Dieses Dokument hält vorläufige Konventionen fest. Es wird konkretisiert,
> sobald mit der Implementierung begonnen wird (nach Freigabe der relevanten ADRs). Verbindliche
> technische Festlegungen erfolgen über **Accepted** ADRs.

## Backend (Java 21 / Spring Boot)

Bezug: [ADR-0003](../decisions/0003-java21-spring-boot-backend.md)

- **Sprachlevel:** Java 21; moderne Features (Records, Sealed Types, Pattern Matching,
  Virtual Threads) bevorzugt, wo sie die Lesbarkeit erhöhen.
- **Architektur:** Kernlogik (Reconciliation) bleibt **frei von Engine-Spezifika**; Container-
  Backends werden über Ports/Adapter angebunden.
  → [container-backend-abstraction](../architecture/container-backend-abstraction.md)
- **Determinismus:** keine impliziten Umgebungsabhängigkeiten in der Kernlogik.
- **Tests:** fachliche Regeln aus `docs/domain` sind durch Tests abzusichern.

## Frontend (Next.js / React)

Bezug: [ADR-0004](../decisions/0004-nextjs-react-readonly-frontend.md)

- **Read-only:** die UI löst keine Infrastrukturänderungen aus.
- **Zweck:** Statuskontrolle und Visualisierung des Plattformzustands.
  → [status-view](../ui/status-view.md)

## Allgemein

- Code darf der Dokumentation nicht widersprechen ([documentation](documentation.md)).
- Verhaltensänderungen ziehen Doku-Aktualisierungen nach sich.
