# Standard — Dokumentation

Dieses Dokument beschreibt die Konventionen der dokumentationsgetriebenen Arbeitsweise. Die
maßgebliche Reihenfolge der Quellen definiert [`CLAUDE.md`](../../CLAUDE.md).

## Source of Truth (absteigend)

1. `CLAUDE.md`
2. Accepted ADRs in `docs/decisions`
3. Domänendokumente in `docs/domain`
4. Geschäftsregeln in `docs/rules`
5. Architektur- und Designdokumente in `docs/architecture`
6. Standards und Richtlinien in `docs/standards`

Code darf der Dokumentation **niemals** widersprechen.

## Ordnerstruktur

```text
docs/
  product/      # Vision, Ideen — SEKUNDÄR, kein Implementierungstreiber
  research/     # Recherche — SEKUNDÄR
  domain/       # Domänenmodell — Implementierungstreiber
  decisions/    # ADRs — nur "Accepted" ist bindend
  rules/        # nur business-rules.md + privacy-rules.md
  architecture/ # Architektur & Design
  standards/    # Standards & Richtlinien
  ui/           # UI-Spezifikationen
```

## Konventionen

- **Sprache:** Fachdokumentation auf Deutsch (konsistent zum bestehenden Bestand).
- **Regeln:** Objektspezifische Regeln in der `## Regeln`-Sektion des Domänenobjekts;
  übergreifende Regeln in `docs/rules/`. Nie eine eigene Regeldatei pro Einzelobjekt anlegen.
- **Keine Technik in Domänendokumenten:** Technische Entscheidungen gehören in ADRs bzw.
  `docs/architecture`, nicht in `docs/domain`.
- **Querverweise:** relative Links zwischen Dokumenten pflegen, damit Zusammenhänge sichtbar sind.
- **Synchronität:** Verändert eine Implementierung das Verhalten, sind Doku, ADR-Referenzen und
  Regeln zu aktualisieren.
