# ADR-0025: Voll-Push-Auslieferung des Sollzustands

Status: Proposed
Datum: 2026-07-13
Betrifft: [ADR-0019](0019-git-config-repository-source.md), [ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md), [ADR-0024](0024-delivered-image-secrets-encrypted-store.md), [ADR-0007](0007-continuous-reconciliation-self-healing.md), [ADR-0001](0001-git-as-single-source-of-truth.md)

> **Ergänzt durch [ADR-0029](0029-docker-compose-single-source-format.md):** Der Push-Mechanismus
> bleibt; die `config`-Nutzlast trägt nun einen **Verzeichnisbaum** (relative Pfade → Inhalt) mit den
> Compose-App-Verzeichnissen statt flacher `{datei: inhalt}`-Paare.

## Kontext

[ADR-0019](0019-git-config-repository-source.md) lässt d4y den Sollzustand aus einem Git-Config-Repo
**ziehen** (Klon + Polling) — d4y braucht dafür Lesezugriff (PAT) und ausgehenden GitHub-Zugriff.
[ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md)/[ADR-0024](0024-delivered-image-secrets-encrypted-store.md)
führten bereits **Push** für Trigger und Secrets ein.

Ziel ist nun der konsequente Voll-Push: **alle Artefakte auf GitHub**, d4y **zieht nichts** und hält
**keine GitHub-Credentials**. Eine GitHub-Actions-Pipeline im Config-Repo liefert **Sollzustand und
Secrets** per authentifiziertem Push an d4y.

## Entscheidung

1. **Config kommt per Push.** Der Trigger-Payload
   ([ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md)) wird erweitert:
   `{ "config": { "<datei>.yaml": "<inhalt>", … }, "secrets": { … } }`. d4y schreibt die gelieferten
   YAML-Dateien in sein Desired-Verzeichnis (`d4y.desired-state.path`) und persistiert sie dort.
2. **d4y zieht nichts.** Ohne gesetzte `d4y.config-repo.url` ist die Git-Anbindung inert; die
   **einzige** Sollzustands-Quelle ist der Push. d4y hält **keine** GitHub-Credentials.
3. **Persistenz + Self-Healing.** Der Reconcile-Loop
   ([ADR-0007](0007-continuous-reconciliation-self-healing.md)) läuft weiter kontinuierlich über das
   **persistierte** Desired-Verzeichnis — d4y heilt sich auch **ohne** neuen Push und **über Neustarts
   hinweg** selbst. Der Push ändert nur *was* der Sollzustand ist.
4. **Konvergenz-Heartbeat statt Polling.** Die Actions-Pipeline pusht bei jeder Config-Änderung **und**
   periodisch (Schedule) — ein Push-Heartbeat ersetzt das d4y-seitige Git-Polling als
   Zustell-Sicherung.
5. **Sicherheit.** Gelieferte Dateinamen werden strikt validiert (kein Pfad-Traversal); es wird nur
   ins Desired-Verzeichnis geschrieben. Secrets bleiben Platzhalter-basiert und verschlüsselt
   ([ADR-0024](0024-delivered-image-secrets-encrypted-store.md)).
6. **Pull-Modus bleibt optional.** Mit gesetzter `d4y.config-repo.url` funktioniert der bisherige
   Pull-Weg ([ADR-0019](0019-git-config-repository-source.md)) unverändert weiter.

## Konsequenzen

- **Positiv:** d4y hält keine GitHub-Credentials und braucht keinen ausgehenden GitHub-Zugriff; das
  Config-Repo bleibt privat und wird nur von Actions berührt. Kein PAT, der leaken kann.
- **Positiv:** Self-Healing bleibt vollständig (persistierter Zustand + Reconcile-Loop), unabhängig von
  der Push-Zustellung.
- **Abweichung von [ADR-0001](0001-git-as-single-source-of-truth.md)/[ADR-0019](0019-git-config-repository-source.md):**
  d4y liest den Sollzustand nicht mehr direkt aus Git. Die **Wahrheit bleibt Git** (Actions rendert
  daraus), d4ys Desired-Verzeichnis ist ein re-derivierbarer, aus dem Push wiederherstellbarer Cache.
- **Negativ:** Fällt die Actions-Pipeline aus, erhält d4y keine Config-Updates (heilt aber weiter aus
  dem letzten Stand); der Schedule-Heartbeat mildert Zustell-Ausfälle.
- **Negativ:** d4y muss für den Push **erreichbar** sein (öffentlicher HTTPS-Endpoint,
  [ADR-0023](0023-push-triggered-reconcile-and-trigger-auth.md)/[ADR-0026](0026-one-liner-bootstrap-github.md)).

## Alternativen

- **Pull aus privatem Repo (PAT)** — verworfen für diesen Modus: d4y hielte wieder ein
  GitHub-Credential und bräuchte ausgehenden Zugriff.
- **Pull aus öffentlichem Repo (anonym)** — verworfen: die Infrastruktur-Topologie wäre öffentlich.
- **Config im RAM ohne Persistenz** — verworfen: Neustart-Lücke widerspricht dem Self-Healing.
