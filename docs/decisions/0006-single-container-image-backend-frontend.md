# ADR-0006: Auslieferung von Backend und Frontend als einzelnes Container-Image

Status: Superseded by [ADR-0027](0027-d4y-host-bundle-systemd.md)
Datum: 2026-07-06
Angenommen: 2026-07-06
Betrifft: [architecture/overview](../architecture/overview.md)

> **Abgelöst durch [ADR-0027](0027-d4y-host-bundle-systemd.md):** d4y wird nicht mehr als
> Container-Image ausgeliefert, sondern als selbst-enthaltendes Host-Bundle (jlink). Der Kern von
> ADR-0006 — Backend **und** Frontend als **ein** Artefakt — bleibt gewahrt (das Frontend ist weiter
> ins Fat-JAR eingebettet, das Bundle bündelt beides).

## Kontext

D4Y besteht aus Backend ([ADR-0003](0003-java21-spring-boot-backend.md)) und Frontend
([ADR-0004](0004-nextjs-react-readonly-frontend.md)). Für die erste Ausbaustufe steht
einfache, reproduzierbare Bereitstellung im Vordergrund.

## Entscheidung

Backend und Frontend werden in der ersten Ausbaustufe **gemeinsam als ein einzelnes
Container-Image** ausgeliefert. Eine spätere Aufteilung in mehrere Komponenten bleibt
ausdrücklich möglich.

## Konsequenzen

- **Positiv:** Minimale Bereitstellungskomplexität, ein Artefakt, reproduzierbar.
- **Positiv:** Weniger bewegliche Teile bei Bootstrap und Betrieb.
- **Negativ:** Backend und Frontend sind vorerst nur gemeinsam skalierbar/versionierbar;
  bei Bedarf per Folge-ADR aufzutrennen.

## Alternativen

- Getrennte Images für Backend und Frontend — für später vorgesehen, für die erste
  Ausbaustufe verworfen (mehr Betriebskomplexität ohne aktuellen Nutzen).
