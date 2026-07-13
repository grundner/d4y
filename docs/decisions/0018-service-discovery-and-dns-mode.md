# ADR-0018: Interne Service-Discovery (d4y-Netz-Aliase) und DNS-Modus-Konfiguration

Status: Accepted
Datum: 2026-07-09
Betrifft: [service-discovery](../domain/service-discovery.md), [dns-provider](../domain/dns-provider.md), [route](../domain/route.md), [architecture/networking-and-dns](../architecture/networking-and-dns.md), [ADR-0010](0010-dns-ingress-service-discovery.md), [ADR-0016](0016-reverse-proxy-traefik-docker-labels.md)

> **Ergänzt durch [ADR-0029](0029-docker-compose-single-source-format.md):** Service-Discovery-Aliase
> entstehen nun über die Mitgliedschaft der Compose-Services im externen `d4y`-Netz (per generiertem
> Override), nicht mehr durch direkte Alias-Injektion beim Container-Create.

## Kontext

[ADR-0010](0010-dns-ingress-service-discovery.md) trennt drei Netzwerk-Ebenen: interne
[Service-Discovery](../domain/service-discovery.md) (App↔App), externen Ingress
([Route](../domain/route.md), umgesetzt in ADR-0016/0017) und autoritative DNS-Records über einen
[DNS-Provider](../domain/dns-provider.md) in **zwei Modi** (managed/extern).

Das gemeinsame `d4y`-Netz aus [ADR-0016](0016-reverse-proxy-traefik-docker-labels.md) legt die
Grundlage für interne Service-Discovery, aber der **stabile logische Name** war noch nicht
umgesetzt: Container tragen nur den App-Namen als Alias, während das Frontend bereits
`<app>.d4y.internal` anzeigt (bislang hartkodiert, nicht auflösbar).

## Entscheidung

1. **Stabiler interner Name = `<app>.<internal-domain>`.** Jeder verwaltete App-Container erhält im
   `d4y`-Netz die Aliase `<app>` **und** `<app>.<internal-domain>`. Die interne Domain ist
   konfigurierbar (`d4y.ingress.internal-domain`, Default `d4y.internal`). Die Auflösung leistet das
   engine-interne DNS des `d4y`-Netzes; Apps kennen keine Server-IPs
   ([service-discovery](../domain/service-discovery.md)).
2. **`serviceName` wird ausgeliefert.** `GET /api/status` liefert je App den stabilen Namen; das
   Frontend zeigt ihn statt einer hartkodierten Konvention.
3. **DNS-Modus konfigurierbar** (`d4y.ingress.dns-mode`, Default `extern`):
   - **extern:** D4Y fasst öffentliches DNS **nicht** an (Betreiber stellt stabilen Eintrittspunkt
     bereit). Dies ist der funktionale Default.
   - **managed:** autoritative Records werden über einen [DNS-Provider](../domain/dns-provider.md)
     verwaltet. Die **konkrete Provider-Integration** (Record-Reconciliation, Entrypoint-IP,
     Credentials) ist eine **eigene Ausbaustufe mit eigener ADR** und noch nicht umgesetzt; ist der
     Modus gesetzt, protokolliert D4Y dies sichtbar und verhält sich weiterhin wie `extern`.
4. **Geheimnisse** (DNS-Provider-Credentials) sind Backend-Konfiguration und erscheinen **nie** in
   UI/API/Statusausgaben ([privacy-rules PR-5](../rules/privacy-rules.md)).

## Konsequenzen

- **Positiv:** Interne Service-Discovery ist real und verifizierbar (App↔App über `<app>.d4y.internal`),
  konsistent mit der UI. Nutzt das bestehende `d4y`-Netz — keine neue Komponente.
- **Positiv:** Der DNS-Modus ist explizit konfigurierbar; `extern` deckt schlanke Setups ab.
- **Negativ:** Der `managed`-Modus ist noch ein dokumentierter Platzhalter — echte Record-Verwaltung
  folgt separat.

## Alternativen

- **Nur App-Name als Alias (kein FQDN)** — verworfen: inkonsistent mit der UI-Konvention und weniger
  eindeutig gegenüber echten Hostnamen.
- **Eigenes internes DNS statt Engine-DNS** — verworfen für den ersten Schnitt: das `d4y`-Netz-DNS
  genügt; eigenes DNS erhöht Komplexität ohne aktuellen Mehrwert.
- **managed-DNS sofort mitimplementieren** — verworfen: provider-spezifisch, nicht lokal
  verifizierbar; verdient eine eigene ADR.
