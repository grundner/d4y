# Standard — ADR-Prozess

Architektur- und Domänenentscheidungen werden als **Architecture Decision Records (ADRs)** in
[`../decisions/`](../decisions/) dokumentiert.

## Status-Lebenszyklus

```text
Proposed ──► Accepted ──► Superseded by ADR-XXXX
   │
   └────────► Rejected
```

- **Proposed** — Entwurf zur Review; **noch nicht bindend**.
- **Accepted** — verbindlich. Code darf einen Accepted ADR niemals stillschweigend verletzen.
- **Superseded** — durch einen neueren ADR ersetzt; nur noch historische Information.
- **Rejected** — verworfen; nur noch historische Information.

## Konventionen

- Dateiname: `NNNN-kurz-beschreibender-titel.md` (fortlaufende, vierstellige Nummer).
- Vorlage: [`../decisions/0000-adr-template.md`](../decisions/0000-adr-template.md).
- Jeder ADR enthält: Status, Datum, Betrifft, Kontext, Entscheidung, Konsequenzen, Alternativen.
- Der [ADR-Index](../decisions/README.md) wird bei jeder Änderung mitgepflegt.

## Regeln

- Erfordert eine Implementierung die Abweichung von einem **Accepted** ADR: stoppen, Konflikt
  erklären, neuen ADR vorschlagen — niemals still abweichen.
- Ein ADR wird erst nach **expliziter Freigabe** durch den Verantwortlichen auf `Accepted` gesetzt.
