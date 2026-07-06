# Domäne — Operative Aktion

Eine **operative Aktion** ist ein imperativer Eingriff im Betrieb, der den **Sollzustand nicht
ändert**. Sie ergänzt das deklarative Modell um Betriebs- und Debugging-Fähigkeiten, ohne die
Git-Wahrheit über den Sollzustand anzutasten.

## Begriff

Operative Aktionen sind **nicht-autoritativ** und **transient**: Sie beschreiben nicht, was
laufen *soll* (das steht ausschließlich im [Config-Repository](config-repository.md)), sondern
greifen punktuell in den Ist-Zustand ein. Alles, was dabei vom Sollzustand abweicht, ist
**sanktionierte, sichtbare Drift** und wird durch die [Reconciliation](reconciliation.md) wieder
aufgelöst — sofern kein [Reconciliation-Hold](reconciliation-hold.md) aktiv ist.

Es werden drei Arten unterschieden:

- **Inspizieren/Debuggen** — Logs ansehen, exec/Shell in einen Container, Container-Details.
  Reines Beobachten, kein Eingriff.
- **Lifecycle-Nudge** — manueller Restart/Stop einer App. Transient; der Reconciler stellt den
  Sollzustand wieder her.
- **Temporärer Parameter** — bewusst vergänglicher Override von Env/Parametern zum Debuggen.
  Erfordert einen [Reconciliation-Hold](reconciliation-hold.md), da er sonst sofort revertiert würde.

## Beziehungen

- Wirkt auf eine [Application](application.md) bzw. deren Container.
- Kann einen [Reconciliation-Hold](reconciliation-hold.md) erfordern (temporäre Parameter, haltbarer Stop).
- Erzeugt keine Änderung am [Desired-vs-Actual-State](desired-vs-actual-state.md)-**Soll**.

## Regeln

- Eine operative Aktion ändert **niemals** den deklarativen Sollzustand; dieser bleibt Git-only.
  → [ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)
- Jede operative Aktion ist **transient**: Abweichungen vom Sollzustand werden von der
  Reconciliation wieder aufgelöst, sobald kein Hold mehr aktiv ist.
- Jede operative Aktion wird **auditiert** und als temporäre Drift **sichtbar** gemacht.
- Aktionen mit Zugriff auf Logs/exec unterliegen den [Privacy-Rules](../rules/privacy-rules.md).
