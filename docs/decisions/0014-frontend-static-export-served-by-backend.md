# ADR-0014: Frontend als statischer Export, ausgeliefert vom Backend

Status: Accepted
Datum: 2026-07-07
Angenommen: 2026-07-07
Betrifft: [ADR-0004](0004-nextjs-react-readonly-frontend.md), [ADR-0006](0006-single-container-image-backend-frontend.md), [architecture/overview](../architecture/overview.md)

## Kontext

[ADR-0006](0006-single-container-image-backend-frontend.md) legt fest, dass Backend und
Frontend als **ein** Container-Image ausgeliefert werden. Offen war der **Mechanismus**:
Läuft im Image ein eigener Next.js-/Node-Server neben dem Backend, oder liefert das Backend
die UI direkt aus?

Das Frontend ([ADR-0004](0004-nextjs-react-readonly-frontend.md)) ist eine reine Client-SPA:
Jede Seite ist `"use client"`, es gibt kein SSR, keine Next.js-API-Routes, keine Server
Actions und keine Middleware. Sämtliche Daten holt der Browser zur Laufzeit per `fetch`
gegen `/api/*`. Ein Next.js-Server erfüllt im Betrieb damit nur noch zwei Aufgaben: statische
Dateien ausliefern und `/api/*` an das Backend proxen (`rewrites`).

## Entscheidung

Das Frontend wird als **statischer Next.js-Export** (`output: "export"`) gebaut und vom
**Spring-Boot-Backend als statische Ressourcen** ausgeliefert. Im Betrieb läuft **kein
Node-/Next.js-Server**.

- UI und `/api/**` teilen sich denselben Port (`server.port`). Dadurch sind die
  Browser-Aufrufe auf `/api/*` **same-origin** — der `rewrites`-Proxy und CORS entfallen.
  Der Proxy bleibt ausschließlich für den lokalen Entwicklungsmodus (`next dev`) erhalten.
- Der Gradle-Build erzeugt den Export und bettet ihn ins Backend-Jar (`/static/`) ein; ein
  `gradlew build` liefert ein einziges Artefakt (konform zu
  [ADR-0002](0002-immutable-images-no-build-on-target.md), kein Build auf dem Ziel).
- Client-Routen werden serverseitig über einen SPA-Resolver aufgelöst
  (`<route>` → `<route>.html`, unbekannt → `index.html`).
- Die App-Detailansicht nutzt einen Query-Parameter (`/applications/detail?name=…`) statt
  einer dynamischen Pfad-Route, da ein statischer Export dynamische Pfad-Segmente ohne zur
  Bauzeit bekannte Werte nicht vorrendern kann.

## Konsequenzen

- **Positiv:** Ein Prozess, ein Port, ein Artefakt; kleineres Image, keine zweite Runtime,
  kein CORS, kein Proxy im Betrieb.
- **Positiv:** Der Frontend-Anwendungscode bleibt nahezu unverändert — die API-Aufrufe sind
  bereits relativ (`/api/...`).
- **Negativ:** Für den Build wird Node/npm benötigt (nur zur Bauzeit, nicht im Image).
- **Negativ / bewusst:** Echte Next.js-Server-Features (SSR, Middleware, API-Routes, Image-
  Optimierung) sind ausgeschlossen. Für die read-only Status-UI irrelevant; die Entscheidung
  ist reversibel (Flag entfernen, Node ins Image aufnehmen) ohne Umbau des App-Codes.

## Alternativen

- **Node/Next.js-Server mit im Image** (`next start`) — erfüllt „ein Image" formal auch, bringt
  aber eine zweite Runtime samt Port-/Prozessverwaltung ohne Nutzen für eine reine Client-UI;
  verworfen.
- **Getrenntes Frontend-Deployment** — widerspricht [ADR-0006](0006-single-container-image-backend-frontend.md)
  für die erste Ausbaustufe; für später nicht ausgeschlossen.
