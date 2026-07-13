# ADR-0017: TLS/HTTPS für Ingress (self-signed Default, ACME opt-in mit HTTP- & DNS-Challenge)

Status: Accepted
Datum: 2026-07-09
Betrifft: [route](../domain/route.md), [dns-provider](../domain/dns-provider.md), [architecture/networking-and-dns](../architecture/networking-and-dns.md), [ADR-0016](0016-reverse-proxy-traefik-docker-labels.md), [ADR-0010](0010-dns-ingress-service-discovery.md)

> **Ergänzt durch [ADR-0027](0027-d4y-host-bundle-systemd.md):** ACME/`le` gilt unverändert auch für
> d4ys eigene Route; diese wird im Host-Betrieb per Traefik-File-Provider deklariert (statt über
> Container-Labels).
>
> **Ergänzt/teilweise abgelöst durch [ADR-0028](0028-per-route-tls-and-http-mode.md):** TLS ist nun
> **pro Route** schaltbar (`tls: true|false`), der **globale HTTP→HTTPS-Redirect entfällt**, und ohne
> ACME ist der Default **HTTP-only** (VM/Intranet ohne öffentliche IP). Punkt 1 (globaler Redirect)
> und Punkt 3 (jede Route zwingend `tls=true`) dieses ADR gelten entsprechend nicht mehr.

## Kontext

Der Ingress läuft aktuell nur über HTTP `:80` ([ADR-0016](0016-reverse-proxy-traefik-docker-labels.md)).
Für echten Betrieb wird **HTTPS** benötigt. Die Zertifikatsquelle ist — wie in
[networking-and-dns](../architecture/networking-and-dns.md) festgehalten — eine eigene Entscheidung:
sie reicht von „einfach lokal testbar" bis „produktionsreife, automatisch erneuerte Zertifikate".

## Entscheidung

1. **HTTPS-Entrypoint `websecure :443`** am verwalteten Traefik zusätzlich zu `web :80` (beide
   host-published). Ein **HTTP→HTTPS-Redirect** ist per Konfiguration schaltbar (Default an).
2. **Zwei Zertifikatsmodi, konfigurierbar** (`d4y.ingress.tls.*`):
   - **Default: self-signed.** Ohne weitere Konfiguration liefert Traefik sein Default-Zertifikat
     aus — HTTPS ist sofort funktional (lokal mit `curl -k` testbar). Kein Produktionsvertrauen.
   - **ACME opt-in** (aktiv, sobald `d4y.ingress.tls.acme.email` gesetzt ist): Let's-Encrypt-artige
     Zertifikate über einen Resolver `le`, mit **HTTP-01**- **oder DNS-01-Challenge**
     (`…acme.challenge = http | dns`). Bei `dns` wird ein Traefik-DNS-Provider
     (`…acme.dns-provider`) plus dessen Zugangsdaten (`…acme.env.*`, als Container-Env)
     verwendet — symmetrisch zum [DNS-Provider](../domain/dns-provider.md) aus ADR-0010. Der
     ACME-Store wird in einem **persistenten Volume** (`d4y_acme` → `/acme`) gehalten. Ein
     alternativer `caServer` (z. B. Staging) ist konfigurierbar.
3. **Pro Route** werden die Traefik-Router auf `websecure` mit `tls=true` gerendert (bei aktivem
   ACME zusätzlich `tls.certresolver=le`). Ist der Redirect deaktiviert, bedienen die Router
   zusätzlich `web` (Plain-HTTP).
4. **Geheimnisse** (DNS-Provider-Credentials) sind Backend-Konfiguration; sie erscheinen **nie** in
   UI, API oder Statusausgaben ([privacy-rules PR-5](../rules/privacy-rules.md)).

## Konsequenzen

- **Positiv:** HTTPS ist ohne Zusatz-Setup sofort funktional und lokal verifizierbar; der
  Produktionspfad (ACME) ist ein Konfigurationsschalter entfernt — inkl. DNS-Challenge für
  Wildcard/interne Hosts ohne öffentliche HTTP-Erreichbarkeit.
- **Positiv:** Nutzt weiterhin die Traefik-/Label-Architektur (ADR-0016); keine zusätzliche
  Komponente.
- **Negativ:** Self-signed erzeugt Browser-Warnungen — nur für Entwicklung/Interim gedacht.
- **Negativ:** ACME erfordert öffentlich auflösbare Domains (HTTP-01) bzw. DNS-Provider-Credentials
  (DNS-01) und einen persistenten Cert-Store.

## Alternativen

- **Nur ACME/Let's Encrypt** — verworfen: nicht lokal verifizierbar, erzwingt Domain+Provider auch
  für einfache/Interim-Setups.
- **Nur statische, bereitgestellte Zertifikate** (File-Provider) — verworfen als Default: verlagert
  Ausstellung/Erneuerung vollständig auf den Betreiber; kann später ergänzt werden.
- **Kein HTTPS / TLS-Terminierung außerhalb** — verworfen: HTTPS gehört zum First-Class-Ingress.
