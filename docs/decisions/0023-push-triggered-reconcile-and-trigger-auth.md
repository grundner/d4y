# ADR-0023: Push-getriggertes Reconcile und authentifizierter Trigger-Endpoint

Status: Proposed
Datum: 2026-07-13
Betrifft: [ADR-0019](0019-git-config-repository-source.md), [ADR-0013](0013-operational-actions-and-hold-api.md), [ADR-0017](0017-tls-https-ingress.md), [ADR-0007](0007-continuous-reconciliation-self-healing.md), [ADR-0024](0024-delivered-image-secrets-encrypted-store.md)

## Kontext

Der Sollzustand wird bisher **gepollt** ([ADR-0019](0019-git-config-repository-source.md): periodischer
`fetch` + `reset --hard`). Für schnellere, ereignisgetriebene Updates — und als Transportweg für
gelieferte Image-Secrets ([ADR-0024](0024-delivered-image-secrets-encrypted-store.md)) — soll eine
externe Instanz (typisch **GitHub Actions** beim Push aufs Config-Repo) d4y **aktiv triggern**.

Ein solcher Endpoint nimmt einen Reconcile-Anstoß (und Secrets) entgegen und **muss** authentifiziert
sein. d4y hat bisher **keine Auth** ([ADR-0013](0013-operational-actions-and-hold-api.md) hat sie
ausdrücklich vertagt). Dieser ADR führt den ersten Auth-Baustein ein — bewusst minimal und nur für
diesen Endpoint.

## Entscheidung

1. **Neuer Endpoint `POST /api/reconcile`.** Stößt sofort `fetch` (falls Git-Modus) + Reconcile an.
   Optionaler Body liefert Secrets (siehe [ADR-0024](0024-delivered-image-secrets-encrypted-store.md)).
   Antwort `202 Accepted`.
2. **Authentifizierung per Bearer-Token.** Ein schmaler Auth-Filter schützt **ausschließlich** diesen
   Endpoint und vergleicht `Authorization: Bearer <token>` konstantzeitig gegen `d4y.trigger.token`
   (ein **host/d4y-Credential** in der Instanz-Konfiguration). Fehlt/stimmt das Token nicht → `401`.
   Ist kein `d4y.trigger.token` gesetzt, ist der Endpoint **deaktiviert** (`404/503`) — fail-closed.
3. **Polling bleibt als Fallback.** Der `@Scheduled`-Poll ([ADR-0019](0019-git-config-repository-source.md))
   bleibt aktiv (ggf. mit längerem Intervall), damit d4y auch ohne Trigger konvergiert.
4. **Exposition & Transport.** Der Endpoint ist von außen erreichbar zu machen und **muss über TLS**
   laufen ([ADR-0017](0017-tls-https-ingress.md)). Runner-IPs sind zu breit für eine IP-Allowlist;
   der Schutz hängt am Token — entsprechend ist ein starkes, rotierbares Token Pflicht.
5. **Kein Auth-Gate für die übrigen Endpoints** in diesem Schnitt — der bestehende `X-Actor`-Ansatz
   ([ADR-0013](0013-operational-actions-and-hold-api.md)) bleibt unverändert. Vollständiges RBAC folgt
   als eigener ADR.

## Konsequenzen

- **Positiv:** Ereignisgetriebenes, schnelles Reconcile; ein sicherer Kanal, über den Secrets geliefert
  werden können ([ADR-0024](0024-delivered-image-secrets-encrypted-store.md)); erster, klar begrenzter
  Auth-Baustein.
- **Positiv:** Fail-closed — ohne konfiguriertes Token ist der Endpoint inaktiv.
- **Negativ:** d4y bekommt erstmals einen von außen erreichbaren, sicherheitskritischen Endpoint →
  erhöhte Angriffsfläche; erfordert TLS und sorgfältige Token-Verwaltung. Eine Sicherheitsprüfung vor
  Produktivnahme ist geboten.
- **Negativ:** Teil-Auth (nur ein Endpoint) ist bewusst inkonsistent zum Rest — als Zwischenschritt
  zum späteren RBAC-ADR akzeptiert.

## Alternativen

- **Reines Polling beibehalten** — verworfen: kein Transportweg für gelieferte Secrets, langsamer.
- **GitHub-Webhook mit HMAC-Signatur statt Bearer-Token** — als spätere Härtung möglich; für den
  ersten Schnitt genügt ein Bearer-Token (einfacher, kein Webhook-Payload-Parsing nötig).
- **mTLS statt Bearer-Token** — stärker, aber deutlich mehr Betriebsaufwand; später nachrüstbar.
