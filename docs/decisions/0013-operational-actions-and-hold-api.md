# ADR-0013: Operative-Aktionen- und Hold-API

Status: Accepted
Datum: 2026-07-07
Angenommen: 2026-07-07
Betrifft: [operational-action](../domain/operational-action.md), [reconciliation-hold](../domain/reconciliation-hold.md), [ADR-0012](0012-operational-actions-and-reconciliation-hold.md), [ADR-0004](0004-nextjs-react-readonly-frontend.md)

## Kontext

[ADR-0012](0012-operational-actions-and-reconciliation-hold.md) (Accepted) beschreibt operative
Aktionen und den zeitlich begrenzten Hold fachlich. Für die Umsetzung braucht es einen konkreten
API- und Speicher-Vertrag im Backend, ohne die Kern-Invariante zu verletzen (keine Aktion ändert
den Sollzustand — nur Git).

## Entscheidung

1. **REST-Endpunkte** unter `/api` (schreibende Aktionen lesen den Akteur aus Header `X-Actor`):
   - `POST /api/apps/{name}/restart` · `POST /api/apps/{name}/stop` (setzt STOP-Hold) ·
     `POST /api/apps/{name}/params` (Env-Override, setzt TEMP_PARAM-Hold) ·
     `GET /api/apps/{name}` (inspect) · `GET /api/apps/{name}/logs` · `POST /api/apps/{name}/exec`
   - `GET /api/holds` · `POST /api/apps/{name}/hold` · `DELETE /api/apps/{name}/hold`
   - `GET /api/activity` · `GET /api/status` wird je App um `hold` erweitert.
2. **Hold ist in-memory und transient.** Er wird nicht persistiert; ein Server-Neustart kehrt zu
   reinem GitOps zurück (Server sind austauschbar). Jeder Hold ist **zeitlich begrenzt** und wird
   auf `hold.max-seconds` geklemmt; er läuft automatisch ab.
3. **Reconciler ist Hold-bewusst.** Gehaltene Ziele werden übersprungen; ein abgelaufener Hold
   wird auditiert (`hold-expired`) und das Ziel wieder normal abgeglichen.
4. **Audit in-memory** (Ring-Puffer): jede mutierende Aktion und jedes Hold-Ereignis wird
   protokolliert (Akteur, App, Aktion, Ergebnis, Drift, Hold) und über `GET /api/activity` sichtbar.
5. **Auth ist aufgeschoben.** Der Akteur kommt aus `X-Actor` (Default `operator`); es gibt kein
   Auth-Gate. Echtes Auth/RBAC folgt als eigener ADR.

## Konsequenzen

- **Positiv:** Die im Frontend gezeigten operativen Aktionen werden real; ADR-0001/0012 bleiben
  gewahrt (Sollzustand unverändert, Holds zeitlich begrenzt, alles auditiert).
- **Positiv:** In-Memory-Hold/-Audit passen zur Austauschbarkeit der Server und sind einfach.
- **Negativ / bekannte Grenzen (dieser Slice):**
  - **Kein Auth** — schreibende Endpunkte sind ungeschützt (nur für vertrauenswürdige Umgebungen;
    Folge-ADR nötig).
  - **Kein Secret-Masking** in Logs/exec-Ausgaben — widerspricht vorerst
    [privacy-rules PR-6](../rules/privacy-rules.md); als Folge-Arbeit dokumentiert.
  - Audit/Hold gehen bei Neustart verloren (bewusst, siehe oben).

## Alternativen

- **Persistenter Hold/Audit** — verworfen: widerspricht der Transienz-Zusage und der
  Server-Austauschbarkeit; erhöht Komplexität ohne aktuellen Nutzen.
- **Auth sofort** — verworfen für diesen Slice: eigenständige Entscheidung, gehört in einen ADR.
- **Reconciler global pausieren statt pro Ziel** — verworfen: andere Ziele sollen selbstheilend
  bleiben (ADR-0012).
