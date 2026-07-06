# ADR-0010: DNS, Ingress und Service-Discovery

Status: Proposed
Datum: 2026-07-06
Betrifft: [route](../domain/route.md), [dns-provider](../domain/dns-provider.md), [service-discovery](../domain/service-discovery.md), [server](../domain/server.md), [architecture/networking-and-dns](../architecture/networking-and-dns.md)

## Kontext

„DNS" umfasst drei getrennte Concerns: interne Service Discovery (App findet App), externen
Ingress (Hostname → App) und autoritative DNS-Records (`host → IP`). Zugleich sind D4Y-Server
austauschbar und haben keine stabile IP ([ADR-0001](0001-git-as-single-source-of-truth.md),
[server](../domain/server.md)) — ein A-Record auf eine konkrete Server-IP würde die
Wegwerfbarkeit brechen. Es braucht ein Modell, das alle drei Ebenen deklarativ abbildet, ohne die
Austauschbarkeit der Server aufzugeben.

## Entscheidung

1. **Ingress als First-Class Route-Konzept.** Hostname→App-Zuordnungen sind ein eigenes,
   deklaratives Domänenobjekt ([Route](../domain/route.md)) im Config-Repository. Die Plattform
   kennt und visualisiert Routes und leitet daraus die Reverse-Proxy-Konfiguration ab.
2. **Konfigurierbare DNS-Verwaltung (zwei Modi).**
   - **managed:** D4Y verwaltet autoritative Records über einen externen
     [DNS-Provider](../domain/dns-provider.md) (deklariert im Config-Repo, symmetrisch zu
     Registry/Backup-Store) und aktualisiert sie auch bei Serverwechsel.
   - **extern:** DNS zeigt auf einen stabilen Eintrittspunkt (Floating IP / externer LB); D4Y
     fasst öffentliches DNS nicht an.
3. **Interne Service Discovery über stabile logische Namen.** Jede App ist über einen stabilen,
   aus ihrer Deklaration abgeleiteten Namen erreichbar; die Plattform-Netzwerkschicht löst
   App-zu-App per Name auf.

In allen Fällen gilt: öffentliche Erreichbarkeit und interne Adressierung hängen **nie** an der
IP eines konkreten Servers.

## Konsequenzen

- **Positiv:** Alle drei Netzwerk-Ebenen sind deklarativ und Git-native; Routes sind sichtbar/
  visualisierbar. Server bleiben austauschbar.
- **Positiv:** Zwei DNS-Modi decken sowohl schlanke Setups (externer Eintrittspunkt) als auch
  vollständig Git-verwaltetes DNS ab.
- **Negativ:** Der managed-Modus erfordert Provider-Zugangsdaten und Record-Reconciliation.
- **Negativ:** Zwei Modi bedeuten mehr Konzept- und Implementierungsaufwand als ein einzelner.

## Alternativen

- **Nur externer Eintrittspunkt** (kein managed DNS) — verworfen: widerspricht der „alles in
  Git"-Vision für Setups ohne externen LB.
- **Nur managed DNS** — verworfen: erzwingt Provider-Anbindung auch dort, wo ein stabiler
  Eintrittspunkt genügt.
- **Reverse Proxy als gewöhnliche App ohne First-Class-Routing** — verworfen: Routing wäre für
  die Plattform intransparent, keine Visualisierung.
- **Keine interne Service Discovery** — verworfen: App-zu-App-Kommunikation wäre nicht
  deklarativ abgesichert.
