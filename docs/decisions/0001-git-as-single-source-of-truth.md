# ADR-0001: Git als einzige Quelle der Wahrheit

Status: Proposed
Datum: 2026-07-06
Betrifft: [config-repository](../domain/config-repository.md), [architecture/overview](../architecture/overview.md)

## Kontext

Infrastruktur wird traditionell durch manuelle Administration der Server verändert, was zu
Drift, fehlender Nachvollziehbarkeit und schwer reproduzierbaren Zuständen führt. D4Y soll
Infrastruktur so reproduzierbar, versionierbar und deterministisch machen wie Software.

## Entscheidung

Der gesamte Sollzustand der Infrastruktur wird **deklarativ in einem Git-Config-Repository**
beschrieben. Git ist die **einzige** Quelle der Wahrheit. Jede Änderung an der Infrastruktur
erfolgt ausschließlich als Änderung in diesem Repository.

## Konsequenzen

- **Positiv:** Versionierung, Nachvollziehbarkeit, Reproduzierbarkeit, Reviewbarkeit von
  Infrastrukturänderungen.
- **Positiv:** Server werden austauschbar und zustandslos.
- **Negativ:** Änderungen an der Live-Infrastruktur außerhalb von Git sind unzulässig — das
  erfordert Disziplin und wird technisch als read-only durchgesetzt (API/UI).

## Alternativen

- Direkte Server-Administration — verworfen (kein Determinismus, kein Audit).
- Zustandsdatenbank als Wahrheit — verworfen (keine native Versionierung/Review wie Git).
