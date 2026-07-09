# Frontend Design Brief — Prompt für „Claude Design"

> **Zweck:** Wiederverwendbarer Prompt, um das D4Y-Frontend zu designen. Sekundäres Arbeitsdokument
> (kein Implementierungstreiber). Verbindlich bleiben die Domänendokumente und **Accepted** ADRs.
> Entwurfsvorgaben: Next.js (App Router) + React + **Material UI**, `@mui/x-data-grid` für Tabellen,
> **Dark-Theme mit definierter Custom-Palette** (Blau/Lila, siehe [ADR-0015](../decisions/0015-frontend-dark-blue-theme.md)),
> möglichst Standard-Komponenten (Anpassung auf Theme-Ebene), zusätzliches CSS nur wo nötig.
> Bezug: [status-view](status-view.md), [ADR-0004](../decisions/0004-nextjs-react-readonly-frontend.md),
> [ADR-0015](../decisions/0015-frontend-dark-blue-theme.md),
> [operational-action](../domain/operational-action.md), [reconciliation-hold](../domain/reconciliation-hold.md).

---

## Auftrag: Frontend-Design für „D4Y" — Git-native Runtime Platform

Entwirf das Frontend für **D4Y**, eine Git-native Runtime-Plattform, die containerisierte
Anwendungen betreibt. Liefere ein zusammenhängendes, hochwertiges UI-Design als **Next.js (App
Router) + React + Material UI**-Implementierung (Screens als Komponenten mit gemeinsamem Layout,
inkl. Loading-/Empty-/Error-Zuständen).

### Produktkontext (kurz)
D4Y liest den **Sollzustand** aus einem Git-Repository und gleicht ihn kontinuierlich mit dem
Ist-Zustand der Container-Engine ab (observe → diff → reconcile), selbstheilend. Das Frontend
dient der **Statuskontrolle/Visualisierung** und erlaubt **operative Aktionen** im Betrieb.

**Zentrale Invariante (unbedingt im Design verankern):**
Das UI ist **read-only bezüglich des Sollzustands** — es kann den deklarativen Sollzustand
(welche Apps, Images, Routes, Volumes) **niemals** ändern; das geschieht ausschließlich über Git.
Erlaubt sind nur **operative Aktionen**, die den Sollzustand nicht ändern; deren Abweichungen sind
**sanktionierte, temporäre Drift** (sichtbar, auditiert, zeitlich begrenzt).

### Harte Tech-Vorgaben (bitte strikt einhalten)
- **Next.js App Router** + React (Client-Components wo nötig, `use client`).
- **Material UI (@mui/material)** als Basis. MUI-Integration für Next.js via
  `@mui/material-nextjs` (`AppRouterCacheProvider`), `CssBaseline`, `ThemeProvider`.
- **Dark-Theme mit definierter Custom-Palette** (Blau/Lila) gemäß
  [ADR-0015](../decisions/0015-frontend-dark-blue-theme.md). Anpassung auf **Theme-Ebene** über
  `createTheme` (Modus `dark`, Palette, Schriften); die MUI-Komponentenstruktur bleibt.
- **Schriften:** IBM Plex Sans (UI/Body), IBM Plex Mono (Code/Terminal), Spectral (optional für
  Headings), via `next/font/google`.
- **Möglichst Standard-Komponenten.** Zusätzliches CSS **nur** wo explizit nötig — bevorzugt über
  das `sx`-Prop für Layout; **keine** separaten CSS-Dateien, außer für explizit als „Custom
  Component" gekennzeichnete Elemente (z. B. Log-/Terminal-Viewer).
- **Tabellen/Listen mit `@mui/x-data-grid`** (Sortieren/Filtern/Paginierung).
- Icons aus `@mui/icons-material`.
- Kein exotisches Custom-Styling, keine Fremd-UI-Bibliotheken.
- Deutsche UI-Texte.

### Design-Prinzipien
- **Sollzustand ist Git-only:** Es gibt **keine** Formulare zum Anlegen/Bearbeiten/Löschen von
  Apps, Routes, Volumes etc. An solchen Stellen steht klar: „Änderung nur über das
  Config-Repository (Git)". Optional ein Deep-Link/Hinweis auf das Repo.
- **Operative Aktionen klar abgegrenzt:** Restart, Stop, temporäre Parameter, Logs, exec/Shell,
  Hold setzen/freigeben. Aktionen, die Drift erzeugen, immer mit sichtbarem Hinweis + Countdown.
- **Ehrliche Zustände:** In Sync / Drift / Reconciling / Gehalten (Hold) / Fehler klar und
  farbcodiert. Fehler nicht verschleiern.
- **Keine Geheimnisse** in der UI (auch nicht in Log-/exec-Ausgaben-Darstellung).

### Informationsarchitektur / Navigation
Persistente linke Navigation (Drawer) + AppBar (Titel „D4Y", Auto-Refresh-Umschalter,
User-Menü als Platzhalter). Seiten:
1. **Dashboard** (`/`)
2. **Applications** (`/applications`, Detail `/applications/[name]`)
3. **Infrastruktur / Topologie** (`/infrastructure`)
4. **Aktivität / Audit** (`/activity`)
5. **Config-Repository** (`/config`, read-only)

### Screens im Detail

#### 1. Dashboard (`/`)
- **Großer Gesamtstatus**: `IN_SYNC | DRIFT | RECONCILING | HOLD | ERROR` (prominent, farbcodiert).
- Kennzahlen-Karten: Apps gesamt, davon In Sync / Drift / Missing / Stopped / Outdated;
  undeclared Container; aktive Holds.
- **Letzte Reconciliation**: Zeitpunkt + Ergebnis. **Config-Repo-Version** (Commit/Ref).
- Liste „Braucht Aufmerksamkeit" (Apps mit Drift/Fehler) → Deep-Link ins App-Detail.
- **Aktive Holds** mit verbleibender Dauer (Countdown) + „Freigeben".
- Kurzer Aktivitäts-Feed (letzte operative Aktionen).

#### 2. Applications (`/applications`)
DataGrid, Spalten: Name, Image, **Status-Chip** (`IN_SYNC/MISSING/STOPPED/OUTDATED`), Läuft (bool),
Route(s) (Hostnamen), Backup (an/aus), Hold (Badge falls aktiv). Suche/Filter. Zeile → Detail.
Diese Seite an die reale API binden — Antwortschema von `GET /api/status`:
```json
{
  "overall": "IN_SYNC | DRIFT",
  "applications": [
    {"name":"nginx","desiredImage":"nginx:1.27-alpine","state":"IN_SYNC|MISSING|STOPPED|OUTDATED","running":true,"containerId":"df24…",
     "volumes":[{"name":"html","path":"/usr/share/nginx/html"}],
     "routes":[{"host":"nginx.example.com","path":"/"}]}
  ],
  "undeclared": [ {"appName":"…","image":"…","containerId":"…","running":true,"volumes":[{"name":"…","path":"/…"}]} ]
}
```
`applications[].volumes` sind die **deklarierten** Named Volumes (Soll); `undeclared[].volumes` die
tatsächlich am Container gemounteten (Ist).

Zusätzlich einen Abschnitt/Tab **„Undeclared Container"** (verwaltete Container ohne Deklaration =
Drift), mit Aktion „Entfernen" (operativ). Zeile → eigene **Detailansicht** des nicht deklarierten
Containers (Image, Container-ID, Läuft, gemountete Volumes; read-only).

#### 3. Application-Detail (`/applications/[name]`)
Header: Name, Status-Chip, Desired Image, Service-Discovery-Name (interner stabiler Name).
**Aktionsleiste (operativ):** „Restart", „Stop" (mit Hold), „Temporäre Parameter" (mit Hold),
„Hold setzen/freigeben". Alle mit Bestätigungsdialog; Aktionen, die Drift erzeugen, weisen auf
den Hold + Countdown hin. **Tabs:**
- **Übersicht:** Soll vs. Ist (Image, Läuft, Container-ID, Server), Sync-State, letzter Reconcile.
- **Logs:** read-only Log-Viewer (Tail/Streaming-Optik) — als **Custom Component** erlaubt.
- **exec / Shell:** Terminal-artige Konsole (Debugging) — **Custom Component** erlaubt.
- **Volumes:** deklarierte Volumes (Named vs. Bind), persistent/ephemer, Backup-Policy (an/aus),
  Backup-Store, letzter Restore. Rein informativ (Änderung nur via Git).
- **Routes:** Hostname→App-Zuordnungen, DNS-Modus (managed / externer Eintrittspunkt). Informativ.
- **Parameter / Hold:** aktive temporäre Parameter-Overrides (transient), aktiver Hold mit
  Restdauer + „Freigeben".

#### 4. Infrastruktur / Topologie (`/infrastructure`)
Karten/Grids für die austauschbaren, zustandslosen Bausteine:
- **Server / Nodes:** ID/Host, Anzahl verwalteter Container, Status. Hinweis „austauschbar/zustandslos".
- **Registries:** vertrauenswürdige Image-Quellen.
- **Backup-Stores:** externe Ziele für Datendurability.
- **DNS-Provider:** (managed-Modus) autoritative Records; sonst „externer Eintrittspunkt".
Beziehungen andeuten: Code←Registry, Daten←Backup-Store, Namen←DNS-Provider (Symmetrie).

#### 5. Aktivität / Audit (`/activity`)
DataGrid: Zeitpunkt, Akteur, Ziel-App, Aktionstyp (`inspect | restart | stop | temp-param |
hold-set | hold-expired`), Ergebnis, Marker „sanktionierte temporäre Drift", Hold-Dauer/Restzeit.
Filterbar nach App/Typ/Zeit.

#### 6. Config-Repository (`/config`, read-only)
Zeigt: aktuelle Version/Commit des Config-Repos, Quelle-der-Wahrheit-Hinweis, ggf. Baum der
deklarierten Objekte. Deutlicher Hinweis: **„Änderungen erfolgen ausschließlich über Git."**

### Statuslogik & Farb-/Chip-Mapping (MUI-`color`)
- `IN_SYNC` → `success` (Häkchen-Icon)
- `DRIFT`, `OUTDATED` → `warning`
- `MISSING`, `ERROR` → `error`
- `RECONCILING` → `info`
- `HOLD` (Gehalten) → `secondary`/neutral + Uhr-Icon + Countdown
Chips (`<Chip>`) für App-States; Gesamtstatus als prominentes Badge/Alert.

### Zustände (überall berücksichtigen)
- **Loading:** `Skeleton` / `CircularProgress`.
- **Empty:** freundliche Leerzustände (z. B. „Keine Applications deklariert — füge sie im
  Config-Repo hinzu").
- **Error:** `Alert severity="error"` mit Retry.
- **Auto-Refresh:** Status wird periodisch via Polling aktualisiert (Umschalter in der AppBar);
  `Snackbar` für Aktionsergebnisse.

### Interaktionen
- Operative Aktionen immer über `Dialog`-Bestätigung; bei Drift-erzeugenden Aktionen Auswahl der
  **Hold-Dauer** (zeitlich begrenzt, läuft automatisch ab) im Dialog.
- Nach Aktion: `Snackbar`-Feedback + Statusaktualisierung; aktive Holds mit Live-Countdown.

### Ausdrücklich NICHT (Non-Goals)
- Keine UI zum Erstellen/Bearbeiten/Löschen von Sollzustand (Apps, Images, Routes, Volumes,
  Backup-Policy) — das ist Git-only.
- Kein Fremd-Styling-Framework, kein Layout-Neubau außerhalb der MUI-Komponentenstruktur. Das
  Dark-Theme und die Custom-Palette (Blau/Lila) sind ausdrücklich gewollt —
  siehe [ADR-0015](../decisions/0015-frontend-dark-blue-theme.md).
- Kein unbefristeter Hold (Hold ist immer zeitlich begrenzt).

### Deliverable
Ein konsistentes Screen-Set (obige Seiten) als Next.js-App-Router-Struktur mit gemeinsamem
Layout (AppBar + Drawer), durchgängig Standard-MUI-Komponenten, `@mui/x-data-grid` für Tabellen,
hellem Default-Theme und minimalem `sx`-Styling. Zeige repräsentative Beispiel-/Mock-Daten
(inkl. der o. g. API-Struktur für den Status).
