# ADR-0012: Operative Aktionen und Reconciliation-Hold

Status: Proposed
Datum: 2026-07-06
Betrifft: [operational-action](../domain/operational-action.md), [reconciliation-hold](../domain/reconciliation-hold.md), [reconciliation](../domain/reconciliation.md), [desired-vs-actual-state](../domain/desired-vs-actual-state.md), [ADR-0001](0001-git-as-single-source-of-truth.md), [ADR-0004](0004-nextjs-react-readonly-frontend.md), [ADR-0007](0007-continuous-reconciliation-self-healing.md)

## Kontext

[ADR-0001](0001-git-as-single-source-of-truth.md) macht Git zur einzigen Quelle der Wahrheit für
den Sollzustand. Der Betrieb erfordert aber **imperative Aktionen**, die den Sollzustand *nicht*
ändern: manueller Restart, Logs/exec zum Debuggen, temporäre Parameter.

Zwei Spannungen sind aufzulösen:
1. Operative Aktionen dürfen nicht zur zweiten Wahrheit über den Sollzustand werden.
2. Der kontinuierliche Reconciler ([ADR-0007](0007-continuous-reconciliation-self-healing.md))
   würde einen manuellen Stop oder temporären Override beim nächsten Durchlauf sofort
   zurücksetzen — Self-Healing „kämpft" gegen den Operator.

## Entscheidung

1. **Operative Aktionen** ([operational-action](../domain/operational-action.md)) sind imperativ,
   **nicht-autoritativ** und **transient**. Sie ändern **niemals** den deklarativen Sollzustand
   (der bleibt Git-only). Modelliert werden:
   - **Inspizieren/Debuggen:** Logs, exec/Shell, Container-Details (reines Beobachten).
   - **Lifecycle-Nudges:** manueller Restart/Stop einzelner Apps.
   - **Temporäre Parameter:** bewusst vergänglicher Override von Env/Parametern.
2. **Reconciliation-Hold** ([reconciliation-hold](../domain/reconciliation-hold.md)): ein
   **zeitlich begrenzter** Hold pausiert die Reconciliation für ein Ziel, damit temporäre
   Overrides/Stops Bestand haben. Der Hold **läuft automatisch ab**; danach greift wieder reines
   GitOps/Self-Healing. Ohne aktiven Hold werden Abweichungen wie bisher sofort korrigiert.
3. **Audit & Sichtbarkeit:** jede operative Aktion und jeder Hold wird protokolliert und als
   **sanktionierte, temporäre Drift** im Status sichtbar gemacht.

## Konsequenzen

- **Positiv:** Betrieb (Restart, Debug-Fenster, temporäre Parameter) ist möglich, ohne ADR-0001
  aufzuweichen — der Sollzustand bleibt Git-only.
- **Positiv:** Der Hold verhindert das „Gegeneinander" von Reconciler und Operator, ohne
  Self-Healing dauerhaft abzuschalten (Zeitbegrenzung erzwingt Rückkehr zu GitOps).
- **Positiv:** Abweichungen sind nie unsichtbar — sie sind auditiert und als temporäre Drift markiert.
- **Negativ:** Zusätzliche Zustände (Hold, Ablauf) und schreibende Endpunkte erhöhen die
  Komplexität und erfordern Zugriffskontrolle. → [privacy-rules](../rules/privacy-rules.md)
- **Negativ:** Während eines Holds ist das betroffene Ziel nicht selbstheilend — die
  Zeitbegrenzung hält dieses Fenster klein.

## Alternativen

- **Kein Hold, sofortiges Revert** — verworfen: kein haltbares Debug-Fenster; nur reine Nudges.
- **Reconciliation für ein Ziel unbefristet abschaltbar** — verworfen: dauerhaft nicht
  selbstheilende Ziele widersprechen dem Plattformziel; deshalb **zeitlich begrenzt**.
- **Operative Aktionen als Sollzustand-Commits nach Git** — verworfen: vermischt transiente
  Betriebsaktionen mit der versionierten Wahrheit.
