# ADR-0019: Sollzustand aus Git-Config-Repository (JGit, HTTPS)

Status: Accepted
Datum: 2026-07-09
Betrifft: [config-repository](../domain/config-repository.md), [desired-vs-actual-state](../domain/desired-vs-actual-state.md), [ADR-0001](0001-git-as-single-source-of-truth.md), [ADR-0006](0006-single-container-image-backend-frontend.md), [ADR-0011](0011-interim-local-desired-state-source.md)

## Kontext

[ADR-0001](0001-git-as-single-source-of-truth.md) (Accepted) legt fest, dass der Sollzustand
ausschließlich aus einem **Git-Config-Repository** stammt. [ADR-0011](0011-interim-local-desired-state-source.md)
war die **bewusste Interimslösung** (lokales `desired/`-Verzeichnis) und sollte abgelöst werden,
sobald die Git-Anbindung steht. Diese ADR beschreibt die Umsetzung.

## Entscheidung

1. **Git-Client: [JGit](https://www.eclipse.org/jgit/) (pure Java).** Kein `git`-Binary im
   Laufzeit-Image nötig — konsistent mit dem Single-Image-Ansatz ([ADR-0006](0006-single-container-image-backend-frontend.md)).
2. **Klonen & Aktualisieren.** Beim Start klont D4Y das konfigurierte Repo in ein lokales
   Arbeitsverzeichnis; periodisch (`d4y.config-repo.poll-interval-ms`) wird `fetch` + `reset --hard`
   auf `origin/<branch>` ausgeführt. Der Klon ist ein **read-only Spiegel** — lokale Abweichungen
   werden verworfen (der Sollzustand ist Git-only, UI/API ändern ihn nie).
3. **Auth: HTTPS.** Öffentliche Repos anonym; private über **Username/Token** (`d4y.config-repo.username`,
   `…token`, z. B. ein PAT). SSH-Deploy-Keys sind eine spätere Erweiterung.
4. **Lesen unverändert.** Der bestehende YAML-Parser liest die Dateien aus `<klon>/<config-repo.path>`
   (Default: Repo-Wurzel) — das deklarative Modell (Apps, Images, Volumes, Routes) bleibt gleich;
   nur die **Bezugsquelle** wechselt von lokal auf Git.
5. **Lokaler Fallback bleibt.** Ist **keine** `d4y.config-repo.url` gesetzt, liest D4Y weiterhin das
   lokale `desired/`-Verzeichnis — praktisch für Entwicklung/Tests. Dieser Modus ist die letzte
   Verwendung der Interims-Idee aus [ADR-0011](0011-interim-local-desired-state-source.md).
6. **Version sichtbar.** Ein neuer Endpoint `GET /api/config` liefert Repo/Branch/Commit/Autor/Zeit
   des aktuellen Stands (bzw. den lokalen Modus); das Frontend zeigt dies statt Mock-Daten.
7. **Geheimnisse.** Der Token ist Backend-Konfiguration und erscheint **nie** in UI/API/Logs
   ([privacy-rules](../rules/privacy-rules.md)).

Damit wird [ADR-0011](0011-interim-local-desired-state-source.md) **abgelöst** (Superseded); der
lokale Modus bleibt als bewusst dokumentierter Entwicklungs-Fallback erhalten.

## Konsequenzen

- **Positiv:** ADR-0001 ist erfüllt — der Sollzustand kommt versioniert aus Git. Kein git-Binary im
  Image. Der Reconcile-Loop arbeitet unverändert gegen den YAML-Parser.
- **Positiv:** Lokaler Fallback erhält die einfache Entwickler-Erfahrung.
- **Negativ:** JGit + Credentials erhöhen die Abhängigkeits-/Angriffsfläche; Token muss sicher
  verwaltet werden.
- **Negativ:** `reset --hard` verwirft lokale Änderungen im Klon bewusst — der Klon ist kein
  Arbeitsverzeichnis für Menschen.

## Alternativen

- **`git`-CLI per Prozess-Aufruf** — verworfen: bräuchte ein git-Binary im Image (widerspricht
  ADR-0006) und Prozess-/Pfad-Handling.
- **SSH-Deploy-Keys jetzt** — verschoben: HTTPS/Token deckt öffentliche und die meisten privaten
  Repos ab und ist lokal (file://) verifizierbar; SSH folgt bei Bedarf.
- **Kein lokaler Fallback** — verworfen: erschwert Entwicklung/Tests ohne Nutzen.
