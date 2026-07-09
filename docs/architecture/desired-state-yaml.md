# Architektur — Desired-State-YAML (Format & Funktionsweise)

Status: Draft
Bezug: [ADR-0011](../decisions/0011-interim-local-desired-state-source.md),
[ADR-0001](../decisions/0001-git-as-single-source-of-truth.md),
[config-repository](../domain/config-repository.md), [reconciliation-loop](reconciliation-loop.md)

D4Y liest den **Sollzustand** — welche Anwendungen mit welchem Image und welchen Volumes laufen
sollen — aus deklarativen **YAML-Dateien**. Dieses Dokument beschreibt deren **Struktur** und wie
D4Y sie **verarbeitet**.

> **Interim (ADR-0011):** In der ersten Ausbaustufe stammen diese Dateien aus einem **lokalen
> Verzeichnis** statt aus einem geklonten Git-Repository. Format und Verarbeitung bleiben gleich,
> sobald die Git-Anbindung folgt; nur die Bezugsquelle ändert sich. Der Sollzustand ist und bleibt
> **read-only für UI und API** ([ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)) —
> Änderungen erfolgen ausschließlich über diese Dateien (später Git).

## Ablage & Erkennung

- Das Verzeichnis wird über die Konfiguration `d4y.desired-state.path` bestimmt (Default:
  `./desired`).
- Alle Dateien mit Endung `*.yaml` oder `*.yml` werden gelesen, in **sortierter** Reihenfolge.
- Existiert das Verzeichnis nicht, gilt ein **leerer** Sollzustand (kein Fehler).

## Dateiform

Jede Datei enthält **entweder** ein einzelnes Application-Objekt **oder** eine **YAML-Liste**
solcher Objekte. Beide Formen sind gleichwertig; die Aufteilung auf Dateien ist frei wählbar.

## Felder

| Feld      | Pflicht | Typ           | Bedeutung |
| --------- | ------- | ------------- | --------- |
| `name`    | ja      | String        | Name der [Application](../domain/application.md); darf nicht leer sein. Der Container heißt `d4y_<name>`. |
| `image`   | ja      | String        | Unveränderliche Image-Referenz `repository:tag` ([ADR-0002](../decisions/0002-immutable-images-no-build-on-target.md)); darf nicht leer sein. |
| `volumes` | nein    | Liste Objekte | Deklarierte **Named Volumes** der App (siehe unten). Fehlt das Feld, hat die App keine Volumes. |

### `volumes[]` — Named Volumes

Jeder Eintrag beschreibt ein **Named Volume** (engine-verwaltet, kein Host-Pfad — der Standard für
persistente Daten; **Bind Mounts sind hier nicht vorgesehen**, siehe
[volume](../domain/volume.md) und [ADR-0009](../decisions/0009-persistence-optional-backup-restore.md)).

| Feld   | Pflicht | Typ    | Bedeutung |
| ------ | ------- | ------ | --------- |
| `name` | ja      | String | Logischer Volume-Name. Muster: `[a-zA-Z0-9][a-zA-Z0-9_.-]*`. Der Engine-Volume-Name lautet `d4y_<app>_<name>`. |
| `path` | ja      | String | **Absoluter** Mount-Pfad im Container (z. B. `/var/lib/data`). |

Regeln:

- `name` **und** `path` sind je Eintrag erforderlich.
- Innerhalb einer App müssen sowohl die `name`- als auch die `path`-Werte **eindeutig** sein
  (Duplikate werden abgelehnt).
- Nur die **Deklaration** ist Teil des Sollzustands, **nicht** der Volume-**Inhalt**
  ([desired-vs-actual-state](../domain/desired-vs-actual-state.md)).

## Beispiel

Einzelne App mit zwei Named Volumes (`desired/nginx.yaml`):

```yaml
name: nginx
image: nginx:1.27-alpine
volumes:
  - name: html
    path: /usr/share/nginx/html
  - name: cache
    path: /var/cache/nginx
```

Mehrere Apps in einer Datei (Listen-Form):

```yaml
- name: web
  image: nginx:1.27-alpine
- name: cache
  image: redis:7
  volumes:
    - name: data
      path: /data
```

## Funktionsweise

Der [Reconciliation-Loop](reconciliation-loop.md) läuft periodisch
(`d4y.reconcile.interval-ms`, Default 15 000 ms) nach dem Muster **observe → diff → reconcile**:

1. **Laden (Soll):** Alle YAML-Dateien werden zum aktuellen Sollzustand eingelesen.
2. **Beobachten (Ist):** D4Y listet die von ihm verwalteten Container (Label `d4y.managed=true`)
   und liest deren Labels `d4y.app`, `d4y.image`, `d4y.volumes`.
3. **Abgleich:** Je deklarierter App wird verglichen. Ein Container **passt**, wenn er läuft **und**
   mit derselben Image-Referenz **und** derselben Volume-Deklaration erzeugt wurde. Andernfalls:
   - kein Container vorhanden → **Start**,
   - Container vorhanden, weicht aber ab (Image, Running-Status **oder** Volumes) → **Replace**
     (stoppen/entfernen, neu erstellen),
   - passt → **Noop**.
   - Verwalteter Container **ohne** Deklaration → **StopAndRemove** (nicht deklarierte Drift; im
     Frontend als „Undeclared" sichtbar).
4. **Named Volumes:** Vor dem Erstellen wird jedes deklarierte Volume **idempotent** angelegt
   (`d4y_<app>_<name>`) und am `path` in den Container gemountet. Beim **Replace** bleibt das Named
   Volume bestehen — die **Daten überleben** den Container-Austausch. Ohne konfiguriertes Backup
   sind die Daten dennoch **ephemer** (Backup ist derzeit nicht implementiert).
5. **Labels:** Neu erstellte Container werden mit `d4y.managed`, `d4y.app`, `d4y.image` und
   `d4y.volumes` (kodierte Deklaration) gestempelt — Grundlage für den Drift-Vergleich im nächsten
   Durchlauf.

Ein aktiver, zeitlich begrenzter [Reconciliation-Hold](../domain/reconciliation-hold.md) setzt den
Abgleich für ein Ziel vorübergehend aus (weder Replace noch StopAndRemove).

## Grenzen & Hinweise

- Der **Inhalt** persistenter Volumes ist **nicht** Teil des Sollzustands; Durability entsteht
  ausschließlich über ein (noch nicht implementiertes) [Backup](../domain/backup.md).
- UI und API sind **read-only** bezüglich des Sollzustands; jede Änderung erfolgt über die Dateien.
- Die lokale Bezugsquelle ist **interim** und endet mit der Git-Anbindung (Sunset in
  [ADR-0011](../decisions/0011-interim-local-desired-state-source.md)).

## Konfiguration

| Schlüssel                   | Default     | Bedeutung |
| --------------------------- | ----------- | --------- |
| `d4y.desired-state.path`    | `./desired` | Verzeichnis der YAML-Dateien. |
| `d4y.reconcile.interval-ms` | `15000`     | Intervall des Reconciliation-Loops in Millisekunden. |

## Referenzen

- [ADR-0011 — Interim lokale Desired-State-Quelle](../decisions/0011-interim-local-desired-state-source.md)
- [ADR-0001 — Git als einzige Quelle der Wahrheit](../decisions/0001-git-as-single-source-of-truth.md)
- [ADR-0002 — Unveränderliche Images](../decisions/0002-immutable-images-no-build-on-target.md)
- [ADR-0009 — Persistenz: optionales Backup/Restore](../decisions/0009-persistence-optional-backup-restore.md)
- [Domäne — Application](../domain/application.md) · [Volume](../domain/volume.md) ·
  [Config-Repository](../domain/config-repository.md)
- [Architektur — Reconciliation-Loop](reconciliation-loop.md)
