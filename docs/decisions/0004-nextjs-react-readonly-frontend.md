# ADR-0004: Frontend auf Basis von Next.js/React, zunächst read-only

Status: Proposed
Datum: 2026-07-06
Betrifft: [architecture/overview](../architecture/overview.md), [ui/status-view](../ui/status-view.md)

## Kontext

Nutzer benötigen Einblick in den aktuellen Plattformzustand (Soll vs. Ist, laufende
Anwendungen, Reconciliation-Status). Da Änderungen ausschließlich über das Git-Config-Repo
erfolgen ([ADR-0001](0001-git-as-single-source-of-truth.md)), darf das Frontend keine
Infrastrukturänderungen auslösen.

## Entscheidung

Das Frontend wird mit **Next.js** und **React** umgesetzt und ist in der ersten Ausbaustufe
**ausschließlich read-only** — Statuskontrolle und Visualisierung des Plattformzustands.

## Konsequenzen

- **Positiv:** Klare Trennung — die Wahrheit bleibt in Git; die UI kann nichts verändern.
- **Positiv:** Bewährtes Frontend-Ökosystem, gute Developer Experience.
- **Negativ:** Bedienung der Infrastruktur ist nur über Git möglich (bewusst gewählt).

## Alternativen

- Schreibende UI mit direkter Infrastruktursteuerung — verworfen (widerspricht ADR-0001).
- Reines Backend-CLI ohne UI — verworfen (Visualisierung ist ein Kernziel).
