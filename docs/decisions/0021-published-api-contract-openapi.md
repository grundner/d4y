# ADR-0021: Publizierter, generierter API-Vertrag (OpenAPI)

Status: Proposed
Datum: 2026-07-10
Betrifft: [ADR-0004](0004-nextjs-react-readonly-frontend.md), [ADR-0013](0013-operational-actions-and-hold-api.md), [ADR-0014](0014-frontend-static-export-served-by-backend.md), [architecture/overview](../architecture/overview.md)

## Kontext

Die REST-API unter `/api` (Status, Config, Aktivität, App-Aktionen, Holds — siehe
[ADR-0013](0013-operational-actions-and-hold-api.md)) wird bisher **nur** vom mitgelieferten,
statischen Frontend konsumiert ([ADR-0014](0014-frontend-static-export-served-by-backend.md):
same-origin, kein CORS). Es gibt **keinen maschinenlesbaren Vertrag**: Die API-Oberfläche ist nur
implizit im Controller-Code beschrieben.

Es entsteht Bedarf für **externe Clients** (z. B. eine native macOS-Companion-App in einem eigenen
Repository), die gegen d4y entwickelt werden. Ein externer Client soll nicht an d4ys Quellcode,
sondern an einen **stabilen, versionierten Vertrag** koppeln, der stets dem tatsächlichen Stand der
API entspricht. Ein handgepflegter Vertrag würde gegenüber dem Code driften.

## Entscheidung

1. **Generierter OpenAPI-Vertrag.** Das Backend bindet **springdoc-openapi** ein und exponiert die
   OpenAPI-Beschreibung der `/api`-Endpunkte automatisch aus den Controllern:
   - `GET /v3/api-docs` (JSON) und `GET /v3/api-docs.yaml` (YAML) an einer laufenden Instanz.
   - Der Vertrag ist damit **immer aktuell** (aus dem Code generiert, nicht handgepflegt).
2. **Committetes Artefakt an stabilem Pfad.** Ein Gradle-Task erzeugt die Spec und schreibt sie nach
   **`docs/api/openapi.yaml`**. Dieses File wird versioniert und in **CI** bei Änderungen neu erzeugt
   (Drift-Schutz), sodass der Vertrag **ohne laufenden Server und von jeder Maschine** per Git-Ref
   abrufbar ist. Damit können externe Clients einen **gepinnten** d4y-Stand konsumieren.
3. **Read-only bleibt read-only.** Der Vertrag beschreibt die bestehende API; er ändert die
   Semantik nicht. [ADR-0004](0004-nextjs-react-readonly-frontend.md) gilt unverändert: der
   Sollzustand wird nie über die API geändert, nur operative Aktionen ausgelöst.
4. **Auth bleibt vorerst offen — wird aber drängender.** [ADR-0013](0013-operational-actions-and-hold-api.md)
   hat Auth ausdrücklich vertagt (kein Gate, Akteur via `X-Actor`, „nur für vertrauenswürdige
   Umgebungen"). Ein **netzwerk-remoter externer Client** verschärft diese Frage. Diese ADR **löst
   sie nicht**, hält den Bedarf aber fest: echte Auth/RBAC folgt als eigener ADR.

## Konsequenzen

- **Positiv:** Externe Clients (Companion-App) koppeln an einen generierten, immer korrekten Vertrag;
  Client-Code (z. B. ein Swift-API-Client) kann daraus erzeugt werden. Brechende API-Änderungen
  werden beim Neugenerieren sichtbar.
- **Positiv:** `docs/api/openapi.yaml` als versioniertes Artefakt macht die API-Oberfläche
  reviewbar und diffbar (Teil der dokumentationsgetriebenen Quelle).
- **Negativ:** Zusätzliche Abhängigkeit (springdoc) und ein Build-/CI-Schritt, der die App zur
  Spec-Erzeugung kurz startet.
- **Negativ:** Ein öffentlicher Vertrag erhöht den Druck, die vertagte Auth-Frage
  ([ADR-0013](0013-operational-actions-and-hold-api.md)) zu adressieren.

## Alternativen

- **Handgepflegte OpenAPI-Datei** — verworfen: driftet gegenüber dem Controller-Code; genau das soll
  ein generierter Vertrag verhindern.
- **Externe Clients lesen d4ys Quellcode direkt** — verworfen: koppelt an Interna statt an einen
  stabilen Vertrag, nicht versioniert/reproduzierbar konsumierbar.
- **Nur Laufzeit-Endpoint ohne committetes Artefakt** — verworfen: setzt eine laufende, erreichbare
  Instanz voraus; ein gepinntes File erlaubt reproduzierbares Bauen externer Clients auch offline.
