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

Frühe Phase — **dokumentationsgetrieben**. Es existiert derzeit **kein Code**; die
Architektur- und Domänengrundlagen werden zuerst in `docs/` festgelegt. Die
Architekturentscheidungen (ADRs) sind aktuell **Proposed** und noch nicht bindend.

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
- **Auslieferung:** Backend + Frontend als ein Image — [ADR-0006](docs/decisions/0006-single-container-image-backend-frontend.md)
