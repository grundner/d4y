# ADR-0007: Kontinuierliche Reconciliation und Self-Healing

Status: Accepted
Datum: 2026-07-06
Angenommen: 2026-07-06
Betrifft: [reconciliation](../domain/reconciliation.md), [desired-vs-actual-state](../domain/desired-vs-actual-state.md), [architecture/reconciliation-loop](../architecture/reconciliation-loop.md)

## Kontext

Ein einmaliges Anwenden des Sollzustands genügt nicht: Container können abstürzen, der
Ist-Zustand kann driften. D4Y soll den Sollzustand dauerhaft und ohne manuelle Eingriffe halten.

## Entscheidung

Die Plattform gleicht den Ist-Zustand **kontinuierlich** mit dem Soll-Zustand ab. Abweichungen
werden automatisch erkannt und korrigiert; der gewünschte Zustand wird ohne manuelle Eingriffe
wiederhergestellt. Die Infrastruktur ist dadurch **selbstheilend**. Der Abgleich ist
**idempotent**.

## Konsequenzen

- **Positiv:** Dauerhafte Übereinstimmung mit dem Soll; automatische Fehlerkorrektur.
- **Positiv:** Kein manuelles Nachsteuern nötig.
- **Negativ:** Laufende Beobachtung/Steuerung erforderlich; Fehlerbehandlung und Backoff
  müssen sorgfältig entworfen werden (siehe reconciliation-loop).

## Alternativen

- Einmaliges Apply ohne fortlaufenden Abgleich — verworfen (kein Self-Healing, Drift bleibt).
- Rein manuelle Korrektur bei Störungen — verworfen (widerspricht dem Plattformziel).
