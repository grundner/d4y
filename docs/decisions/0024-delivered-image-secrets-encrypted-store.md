# ADR-0024: Gelieferte Image/Container-Secrets und verschlüsselter lokaler Secret-Store

Status: Proposed
Datum: 2026-07-13
Betrifft: [ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md), [ADR-0001](0001-git-as-single-source-of-truth.md), [ADR-0002](0002-immutable-images-no-build-on-target.md), [registry](../domain/registry.md), [privacy-rules](../rules/privacy-rules.md)

> **Ergänzt durch [ADR-0029](0029-docker-compose-single-source-format.md):** Der verschlüsselte
> Secret-Store bleibt; aufgelöste Secrets werden nun als **Umgebungsvariablen an den `docker
> compose`-Prozess** übergeben (native `${VAR}`/`env_file`-Interpolation, nie im Klartext auf Platte).
> Die `${secret:NAME}`-Syntax entfällt.

## Kontext

Secrets, die **Container/Images** betreffen — Registry-Zugangsdaten, App-Env-Secrets — sollen
**weder im Git-Config-Repo noch in d4ys Properties** liegen. Sie gehören nach außen, typischerweise in
**GitHub-Actions-Secrets** (die einzige Wahrheit). GitHub-API-seitig sind diese Secret-**Werte nicht
lesbar** (nur schreibbar); *innerhalb* eines Actions-Runs sind sie aber verfügbar. Daher liefert ein
Actions-Run die Werte per authentifiziertem Push
([ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md)) an d4y.

Da d4y kontinuierlich selbstheilt ([ADR-0007](0007-continuous-reconciliation-self-healing.md)) und
jederzeit private Images ziehen können muss — auch nach einem Neustart — dürfen die Secrets nicht nur
im RAM liegen (sonst Lücke bis zum nächsten Push).

## Entscheidung

1. **Platzhalter im Sollzustand.** YAML referenziert Secrets über `${secret:NAME}` (in App-`env`-Werten
   und in Registry-Passwörtern) — **nie den Klartext**. Das Config-Repo bleibt geheimnisfrei.
2. **Lieferung per Push.** Der Trigger-Call
   ([ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md)) transportiert die Werte als
   `{ "secrets": { "NAME": "wert" } }` über TLS. Voller Satz je Push (idempotenter Replace).
3. **Verschlüsselter lokaler Secret-Store.** d4y hält die Werte im RAM **und** persistiert sie
   **AES-GCM-verschlüsselt** als Datei (`d4y.secrets.file`). Der Schlüssel stammt aus
   `d4y.secrets.encryption-key` — ein **host/d4y-Credential** in der Instanz-Konfiguration (die einzige
   Ausnahme von „keine Image-Secrets in Properties": es ist der Schlüssel, nicht die Image-Secrets).
   Beim Start wird die Datei geladen → nach Neustart sind die Secrets sofort verfügbar, **ohne** erneuten
   Push.
4. **Auflösung zur Reconcile-Zeit.** Beim Laden des Sollzustands werden `${secret:NAME}` aus dem Store
   ersetzt. Ein **unauflösbarer** Platzhalter führt zu einem klaren Fehler (App wird übersprungen und
   auditiert) — nie geht ein Platzhalter an die Engine.
5. **Verwendung.** Aufgelöste Werte speisen die Registry-Authentifizierung (`X-Registry-Auth` beim Pull)
   und App-Env-Secrets.
6. **Geheimhaltung.** Secret-Werte und der `X-Registry-Auth`-Header werden **nie** geloggt oder über
   `/api/*` ausgegeben ([privacy-rules](../rules/privacy-rules.md)).

## Konsequenzen

- **Positiv:** Image/Container-Secrets liegen nie im Repo und nie in Properties; die Wahrheit bleibt in
  GitHub. Neustart-sicher durch den verschlüsselten Cache. Ein einheitlicher Platzhalter-Mechanismus
  deckt Registry-Creds und App-Env-Secrets ab.
- **Abweichung von [ADR-0001](0001-git-as-single-source-of-truth.md):** d4y erhält erstmals persistenten
  lokalen Zustand außerhalb des Git-Mirrors. Bewusst akzeptiert: Der Store ist ein **re-derivierbarer,
  verschlüsselter Cache** (aus GitHub per Push wiederherstellbar), analog zum wegwerfbaren JGit-Mirror —
  keine autoritative Wahrheit.
- **Negativ:** Die Datei-Verschlüsselung schützt **at rest** (Schlüssel im Env schützt nicht gegen einen
  vollständigen Host-Kompromiss). App-Env-Secrets sind zur Laufzeit ohnehin im Container sichtbar
  (`docker inspect`) — der Schutz betrifft Repo, Transport und Ablage.
- **Negativ:** Frischinstanz benötigt einen ersten Push, bevor private Pulls gelingen.

## Alternativen

- **Reines RAM (kein Datei-Cache)** — verworfen: Neustart-Lücke widerspricht dem Self-Healing.
- **Image-Secrets als Instanz-Config/Properties** — verworfen per Anforderung (nur host/d4y-Creds in
  Properties).
- **age/SOPS-verschlüsselt im Config-Repo** — verworfen: Secrets sollen gar nicht im Repo liegen.
- **Externer Secret-Manager (Vault o. ä.) per Read-API** — als spätere, pluggbare Provider-Variante
  möglich; für diesen Schnitt ist der GitHub-Push-Weg gewünscht.
