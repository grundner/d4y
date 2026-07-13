# ADR-0028: Pro-Route-TLS und HTTP-Betrieb ohne öffentliche IP

Status: Proposed
Datum: 2026-07-13
Betrifft: [ADR-0017](0017-tls-https-ingress.md), [ADR-0016](0016-reverse-proxy-traefik-docker-labels.md), [ADR-0027](0027-d4y-host-bundle-systemd.md), [route](../domain/route.md), [architecture/networking-and-dns](../architecture/networking-and-dns.md), [architecture/desired-state-yaml](../architecture/desired-state-yaml.md)

## Kontext

[ADR-0017](0017-tls-https-ingress.md) macht HTTPS zum First-Class-Ingress: jede Route wird auf
`websecure` mit `tls=true` gerendert, ein HTTP→HTTPS-Redirect ist **global** am `web`-Entrypoint
schaltbar (Default an), und ACME ist opt-in (self-signed als Default). Ein reiner HTTP-Betrieb wurde
in ADR-0017 **ausdrücklich verworfen**.

Das verhindert zwei reale Szenarien:

- **VM/Intranet ohne öffentliche IP.** ACME (HTTP-01) braucht öffentliche Erreichbarkeit; ohne sie
  bleibt nur self-signed HTTPS — aber d4y und Apps sollen dort schlicht über **HTTP** laufen.
- **TLS pro Anwendung.** Manche Apps sollen HTTPS, andere (z. B. lokale Tests, interne Tools) sollen
  über **HTTP** erreichbar sein. Heute ist TLS global und uniform.

Der globale HTTP→HTTPS-Redirect verträgt sich zudem **nicht** mit reinen HTTP-Routen: er würde jede
`web`-Route nach `websecure` umleiten.

## Entscheidung

1. **TLS pro Route.** Die [Route](../domain/route.md) erhält ein optionales Feld `tls`
   (`true`/`false`/nicht gesetzt). Im Desired-State-YAML: `tls: true|false` je Route-Eintrag.
   - `tls=true` → Router auf `websecure`, `tls=true` (+ `tls.certresolver=le` bei aktivem ACME).
   - `tls=false` → Router auf `web`, **reines HTTP**.
   - nicht gesetzt → globaler Default (Punkt 3).

2. **Kein globaler HTTP→HTTPS-Redirect mehr.** Der Entrypoint-Redirect entfällt (samt
   `d4y.ingress.https-redirect`). Jede Route bedient genau **einen** Entrypoint (`web` **oder**
   `websecure`), abhängig von ihrem effektiven TLS. `web:80` und `websecure:443` bleiben beide am
   Traefik definiert.

3. **Globaler TLS-Default aus ACME abgeleitet.** `d4y.ingress.tls.default-enabled` (Boolean) ist der
   Fallback für Routen ohne eigene Angabe. Ist er **nicht** gesetzt, gilt: **ACME konfiguriert ⇒
   Default HTTPS**, **sonst ⇒ Default HTTP-only**. Damit läuft eine VM ohne ACME-Mail
   out-of-the-box über HTTP. Explizit `true` erzwingt HTTPS auch ohne ACME (self-signed).

4. **d4ys Selbst-Route (ADR-0027) ist ebenso schaltbar.** `d4y.ingress.self.tls` (Boolean, Default =
   globaler TLS-Default). Ohne TLS wird die File-Provider-Route auf `web` ohne `tls`-Key geschrieben —
   d4y ist dann per `http://<host>/` erreichbar.

5. **Installer.** `D4Y_ACME_EMAIL` wird **optional**. Leer ⇒ kein ACME ⇒ HTTP-only (d4y + Apps sofort
   per HTTP). Gesetzt ⇒ ACME/HTTPS wie bisher. `D4Y_HOST` bleibt Pflicht (Host-Regel im Router; im
   Intranet ein interner Name/IP via DNS oder `/etc/hosts`).

Diese ADR **ergänzt/teilweise ablöst** [ADR-0017](0017-tls-https-ingress.md) (Wegfall des globalen
Redirects und der Zwangs-TLS pro Route; Wiedereinführung eines HTTP-Modus).

## Konsequenzen

- **Positiv:** d4y funktioniert in einer VM/Intranet ohne öffentliche IP über HTTP; TLS ist pro
  Anwendung wählbar; lokale Tests brauchen kein Zertifikat.
- **Positiv:** Vorhersagbares Modell — eine Route ist entweder HTTP oder HTTPS, kein impliziter
  Redirect.
- **Negativ:** Kein automatischer HTTP→HTTPS-Redirect mehr. Wer beides will, deklariert zwei Routen
  oder es kommt später eine per-Route-Redirect-Option (Traefik `redirectscheme`-Middleware).
- **Negativ:** Das `d4y.routes`-Label/Drift-Format trägt jetzt zusätzlich `tls`
  (`host|path|port|tls`, abwärtstolerant beim Dekodieren). Eine Änderung an `tls` ist Drift und
  ersetzt den Container.
- **Sicherheit:** HTTP-only ist für nicht-öffentliche Umgebungen gedacht. Für Produktion mit
  öffentlicher Erreichbarkeit ACME-Mail setzen (HTTPS).

## Alternativen

- **Redirect pro TLS-Route via Middleware beibehalten** — verworfen für diesen Schnitt zugunsten des
  einfacheren „eine Route = ein Entrypoint"-Modells; als spätere Option offen.
- **Globales `http-only`-Flag statt pro-Route-TLS** — verworfen: unflexibel, deckt „manche HTTP,
  manche HTTPS" nicht ab.
- **Bei ADR-0017 bleiben (nur self-signed ohne ACME)** — verworfen: `curl -k`/self-signed ist kein
  echtes HTTP und löst den No-Public-IP-Fall nicht sauber.
