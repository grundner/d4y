# Domäne — DNS-Provider

Ein **DNS-Provider** ist das externe, vertrauenswürdige Ziel, in dem D4Y autoritative
öffentliche DNS-Records verwaltet — im **managed-Modus** der DNS-Verwaltung.

## Begriff

Der DNS-Provider ist zur Namensauflösung das, was die [Registry](registry.md) zu Images und der
[Backup-Store](backup-store.md) zu Daten ist: eine externe, vertrauenswürdige Instanz, die
deklarativ aus dem [Config-Repository](config-repository.md) angesteuert wird.

Die DNS-Verwaltung ist **konfigurierbar** — es gibt zwei Modi:

- **managed** — D4Y verwaltet die autoritativen Records über einen DNS-Provider und aktualisiert
  sie deklarativ aus dem Config-Repository, auch bei Wechsel des [Servers](server.md). Der
  DNS-Provider ist in diesem Modus erforderlich.
- **extern** — die öffentliche Auflösung zeigt auf einen stabilen Eintrittspunkt (z. B. Floating
  IP / externer Load Balancer), den der Betreiber bereitstellt. D4Y fasst öffentliches DNS
  **nicht** an; ein DNS-Provider wird dann nicht benötigt.

## Beziehungen

- Löst die Hostnamen von [Routes](route.md) autoritativ auf (nur im managed-Modus).
- Wird über das [Config-Repository](config-repository.md) referenziert.
- Symmetrisch zu [Registry](registry.md) und [Backup-Store](backup-store.md).

## Regeln

- Ein DNS-Provider ist ein **externes**, vertrauenswürdiges Ziel und wird **deklarativ** im
  Config-Repository referenziert.
- Ein DNS-Provider ist **nur im managed-Modus** erforderlich; im externen Modus verwaltet D4Y
  keine öffentlichen DNS-Records.
- Autoritative Records werden ausschließlich aus dem Sollzustand abgeleitet; sie hängen **nicht**
  von der IP eines konkreten Servers ab.
- Zugangsdaten des DNS-Providers sind vertraulich zu behandeln.
  → [Privacy-Rules](../rules/privacy-rules.md)
