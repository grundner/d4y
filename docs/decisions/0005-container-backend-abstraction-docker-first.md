# ADR-0005: Container-Backend-Abstraktion, Docker als erste Implementierung

Status: Accepted
Datum: 2026-07-06
Angenommen: 2026-07-06
Betrifft: [architecture/container-backend-abstraction](../architecture/container-backend-abstraction.md)

## Kontext

D4Y soll Container ausführen, sich aber nicht dauerhaft an eine einzige Engine binden. Die
erste Implementierung nutzt Docker; künftig sollen weitere Backends möglich sein.

## Entscheidung

Die Steuerung von Containern erfolgt über eine **engine-neutrale Abstraktion** (Port). Die
Kernlogik kennt keine Engine-Spezifika. Die **erste Implementierung** (Adapter) verwendet
**Docker**. Weitere Backends werden als zusätzliche Adapter ergänzt, ohne die Kernlogik zu ändern.

## Konsequenzen

- **Positiv:** Austauschbarkeit der Engine; Kernlogik bleibt testbar und engine-frei.
- **Positiv:** Klare Isolation der Docker-Spezifika in einem Adapter.
- **Negativ:** Zusätzliche Abstraktionsebene; der Port muss engine-neutral gehalten werden,
  auch wenn zunächst nur Docker existiert.

## Alternativen

- Direkte Docker-Anbindung ohne Abstraktion — verworfen (spätere Backends schwer nachrüstbar).
- Sofortige Mehr-Engine-Unterstützung — verworfen (unnötiger Aufwand für erste Ausbaustufe).
