# UI — Status-View (Spezifikation)

Status: Draft
Bezug: [ADR-0004](../decisions/0004-nextjs-react-readonly-frontend.md)

Die Status-View ist die zentrale Funktion des Frontends: Statuskontrolle und Visualisierung des
aktuellen Plattformzustands. Sie ist **read-only bezüglich des Sollzustands** — sie kann den
deklarativen Sollzustand nicht ändern (das bleibt Git). Ergänzend bietet das Frontend
[operative Aktionen](../domain/operational-action.md) (siehe unten), die den Sollzustand nicht
verändern.

## Zweck

- Sichtbar machen, ob die Plattform **In Sync** ist oder **Drift** vorliegt.
  → [reconciliation](../domain/reconciliation.md)
- Überblick über laufende [Applications](../domain/application.md) und deren Zustand.
- Gegenüberstellung von **Soll** und **Ist**.
  → [desired-vs-actual-state](../domain/desired-vs-actual-state.md)

## Darzustellende Informationen (fachlich)

| Bereich | Inhalt |
| --- | --- |
| Plattformstatus | In Sync / Drift erkannt / Reconciling / Gehalten (Hold) / Fehler |
| Anwendungen | laufende Applications, referenziertes Image, Ist-Zustand |
| Soll vs. Ist | Abweichungen (Drift) sichtbar gemacht |
| Operative Aktionen / Holds | aktive Holds, temporäre Overrides, verbleibende Dauer |
| Config-Bezug | zugrunde liegende Version des Config-Repositories |
| Letzte Reconciliation | Zeitpunkt/Ergebnis des letzten Durchlaufs |

## Operative Aktionen

Das Frontend darf [operative Aktionen](../domain/operational-action.md) auslösen, die den
**Sollzustand nicht verändern** und als **temporäre, sanktionierte Drift** dargestellt werden:

- **Inspizieren/Debuggen** — Logs, exec/Shell, Container-Details.
- **Lifecycle-Nudges** — manueller Restart/Stop einzelner Apps.
- **Temporäre Parameter** — vergänglicher Override, abgesichert durch einen zeitlich begrenzten
  [Reconciliation-Hold](../domain/reconciliation-hold.md).

Aktive Holds und laufende Overrides werden sichtbar gemacht — inklusive verbleibender Dauer, nach
der die Plattform automatisch zum Sollzustand zurückkehrt.
→ [ADR-0012](../decisions/0012-operational-actions-and-reconciliation-hold.md)

## Prinzipien

- **Read-only bzgl. Sollzustand:** keine UI-Aktion ändert den deklarativen Sollzustand (nur Git).
- **Operative Aktionen sichtbar:** jede Abweichung durch operative Aktionen ist als temporäre
  Drift erkennbar und wird auditiert.
- **Keine Geheimnisse:** niemals Zugangsdaten/Secrets anzeigen (auch nicht in Logs/exec-Ausgaben).
  → [privacy-rules](../rules/privacy-rules.md)
- **Ehrliche Darstellung:** Fehlerzustände werden sichtbar gemacht, nicht verschleiert.

> Konkrete Layouts, Komponenten und die API-Anbindung sind **Implementierungsdetails** und
> werden bei der Umsetzung festgelegt.
