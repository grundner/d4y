# ADR-0015: Frontend Dark-Theme mit bewusster Blau-/Lila-Palette

Status: Accepted
Datum: 2026-07-08
Betrifft: [ui/frontend-design-brief](../ui/frontend-design-brief.md), [ui/status-view](../ui/status-view.md), [ADR-0004](0004-nextjs-react-readonly-frontend.md), [ADR-0014](0014-frontend-static-export-served-by-backend.md)

## Kontext

Die bisherige Frontend-Vorgabe ([frontend-design-brief](../ui/frontend-design-brief.md)) verlangte
ein **helles MUI-Default-Theme ohne eigene Palette** und führte ein Dark-Theme sowie eine
Custom-Palette ausdrücklich als Non-Goal. Damit hatte das Frontend keine eigenständige visuelle
Identität — das dargestellte Blau war lediglich der MUI-Default `#1976d2`.

Für D4Y ist eine **bewusste, wiedererkennbare visuelle Identität** gewünscht. Referenz ist das
Design „D4Y m9r" (Claude-Design-Projekt „D4Y Design"): ein **dunkles Theme mit klarer Blau-/
Lila-Akzentwelt** und eigenen Schriften (IBM Plex / Spectral). Diese Richtung widerspricht der
bisherigen Vorgabe und muss daher als Entscheidung dokumentiert werden.

## Entscheidung

Das Frontend erhält ein **Dark-Theme mit definierter Custom-Palette**, umgesetzt auf
**Theme-Ebene**: Die bestehende Struktur aus Standard-MUI-Komponenten und `@mui/x-data-grid`
bleibt erhalten; geändert werden Palette, Modus und Schriften über `createTheme`. Kein Layout-
Neubau, kein Fremd-Styling-Framework.

**Palette (verbindliche Werte, gemappt auf MUI-Tokens):**

| Rolle | Hex | MUI-Token |
| --- | --- | --- |
| Hintergrund (default) | `#0e1013` | `background.default` |
| Fläche / Paper | `#161b22` | `background.paper` |
| Primär (Blau) | `#4db8ff` (light `#6ac4ff`, contrastText `#06202e`) | `primary` |
| Sekundär (Lila) | `#b28dff` (light `#c3a9ff`) | `secondary` |
| Success (Grün) | `#5fd0a8` | `success.main` |
| Warning (Amber) | `#e0a94a` | `warning.main` |
| Error (Rot) | `#f4776b` | `error.main` |
| Text primär / sekundär | `#e6eaf0` / `#8b93a1` | `text.primary` / `text.secondary` |
| Divider / Border | `#262c35` | `divider` |

**Schriften:** IBM Plex Sans (UI/Body), IBM Plex Mono (Code/Monospace, Log-/exec-Viewer),
Spectral (optional für Headings) — via `next/font/google`.

Das **Status-/Chip-Mapping** (IN_SYNC=`success`, DRIFT/OUTDATED=`warning`, MISSING/ERROR=`error`,
RECONCILING=`info`, HOLD=`secondary`) bleibt unverändert und folgt der neuen Palette automatisch
über die MUI-`color`-Tokens.

## Konsequenzen

- **Positiv:** Eigenständige, konsistente visuelle Identität; Statusfarben bleiben semantisch,
  nur die konkreten Farbwerte ändern sich.
- **Positiv:** Geringes Risiko — Komponentenstruktur, `@mui/x-data-grid` und der statische Export
  ([ADR-0014](0014-frontend-static-export-served-by-backend.md)) bleiben unberührt.
- **Negativ:** Hebt die bisherige Vorgabe „nur helles Theme / MUI-Default" auf; der
  [frontend-design-brief](../ui/frontend-design-brief.md) wird entsprechend angepasst.
- **Negativ:** Wenige hartcodierte Hex-Werte (Aktions-Chips in `activity`, Hold-/Mismatch-Banner
  im App-Detail) müssen an die Dark-Palette bzw. auf Theme-Tokens umgestellt werden.

## Alternativen

- **Helles MUI-Default-Theme beibehalten** (bisherige Vorgabe) — verworfen: keine eigenständige
  visuelle Identität.
- **Voll originalgetreuer Layout-Neubau** des m9r-Designs (Menubar + Polling-Bar + linke Rail,
  Custom-Komponenten statt MUI) — verworfen (für diese Ausbaustufe): großer Umbau, verwirft viel
  MUI-Standard und `@mui/x-data-grid` ohne proportionalen Mehrwert gegenüber der Theme-Ebene.

---

**Status-Werte:** `Proposed` → `Accepted` → ggf. `Superseded by ADR-XXXX` / `Rejected`.
Nur **Accepted** ADRs sind bindend (siehe [../standards/adr.md](../standards/adr.md)).
