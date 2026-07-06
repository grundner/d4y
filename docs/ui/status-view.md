# UI — Status-View (Spezifikation)

Status: Draft
Bezug: [ADR-0004](../decisions/0004-nextjs-react-readonly-frontend.md)

Die Status-View ist die erste und in der ersten Ausbaustufe **einzige** Funktion des Frontends:
Statuskontrolle und Visualisierung des aktuellen Plattformzustands. Sie ist **read-only** und
kann keine Infrastrukturänderungen auslösen.

## Zweck

- Sichtbar machen, ob die Plattform **In Sync** ist oder **Drift** vorliegt.
  → [reconciliation](../domain/reconciliation.md)
- Überblick über laufende [Applications](../domain/application.md) und deren Zustand.
- Gegenüberstellung von **Soll** und **Ist**.
  → [desired-vs-actual-state](../domain/desired-vs-actual-state.md)

## Darzustellende Informationen (fachlich)

| Bereich | Inhalt |
| --- | --- |
| Plattformstatus | In Sync / Drift erkannt / Reconciling / Fehler |
| Anwendungen | laufende Applications, referenziertes Image, Ist-Zustand |
| Soll vs. Ist | Abweichungen (Drift) sichtbar gemacht |
| Config-Bezug | zugrunde liegende Version des Config-Repositories |
| Letzte Reconciliation | Zeitpunkt/Ergebnis des letzten Durchlaufs |

## Prinzipien

- **Read-only:** keine Buttons/Aktionen, die Infrastruktur verändern.
- **Keine Geheimnisse:** niemals Zugangsdaten/Secrets anzeigen.
  → [privacy-rules](../rules/privacy-rules.md)
- **Ehrliche Darstellung:** Fehlerzustände werden sichtbar gemacht, nicht verschleiert.

> Konkrete Layouts, Komponenten und die API-Anbindung sind **Implementierungsdetails** und
> werden bei der Umsetzung festgelegt.
