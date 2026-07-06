# ADR-0002: Unveränderliche Images, kein Build auf dem Zielsystem

Status: Proposed
Datum: 2026-07-06
Betrifft: [container-image](../domain/container-image.md), [application](../domain/application.md), [registry](../domain/registry.md)

## Kontext

Werden Anwendungen auf dem Zielsystem gebaut, hängt das Ergebnis von der lokalen Umgebung ab
(Toolchain, Abhängigkeiten, Zeitpunkt). Das verhindert deterministische Deployments.

## Entscheidung

Anwendungen werden **niemals auf dem Zielsystem gebaut**. Sie werden ausschließlich als
**unveränderliche Container-Images** aus **vertrauenswürdigen Registries** bereitgestellt.
Dieselbe Image-Referenz liefert in jeder Umgebung denselben Inhalt.

## Konsequenzen

- **Positiv:** Deterministische, reproduzierbare Deployments über alle Umgebungen hinweg.
- **Positiv:** Klare Trennung von Build (extern, CI) und Runtime (D4Y).
- **Negativ:** Es wird eine externe Build-Pipeline und Registry vorausgesetzt; D4Y baut nicht.

## Alternativen

- Build auf dem Zielsystem — verworfen (nicht deterministisch).
- Veränderliche/„latest"-Tags ohne feste Referenz — verworfen (nicht reproduzierbar).
