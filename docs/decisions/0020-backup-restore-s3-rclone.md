# ADR-0020: Backup/Restore von Volumes in einen S3-Backup-Store (rclone-Helfer)

Status: Accepted
Datum: 2026-07-10
Betrifft: [backup](../domain/backup.md), [backup-store](../domain/backup-store.md), [volume](../domain/volume.md), [architecture/persistence-and-backup](../architecture/persistence-and-backup.md), [ADR-0009](0009-persistence-optional-backup-restore.md)

## Kontext

[ADR-0009](0009-persistence-optional-backup-restore.md) legt fest, dass Volume-Daten **optional pro
App** aktiv gesichert werden und ein **Restore nur bei leerem/neuem Volume** erfolgt.
[persistence-and-backup](../architecture/persistence-and-backup.md) hat **Cadence, Protokoll und
Tooling** ausdrücklich einer eigenen ADR überlassen. Diese ADR fixiert den konkreten Mechanismus.

## Entscheidung

1. **Backup-Store: S3-kompatibler Object-Store** (echter externer Store, symmetrisch zur
   [Registry](../domain/registry.md)). Konfiguriert über `d4y.backup.s3.*` (Endpoint, Bucket,
   Region, Provider, Access-/Secret-Key). Lokal mit **MinIO** verifizierbar, produktiv mit AWS S3 &
   Kompatiblen.
2. **Mechanismus: ephemere rclone-Helfer-Container.** D4Y führt Backup/Restore **nicht** im
   Backend-Prozess aus, sondern startet je Operation einen kurzlebigen `rclone`-Container, der das
   **Named Volume** (unter `/data`) mit dem S3-Ziel synchronisiert:
   - **Backup:** `rclone sync /data <store>:<bucket>/<app>/<vol>` (Verzeichnis-Sync, kein tar).
   - **Restore:** `rclone sync <store>:<bucket>/<app>/<vol> /data`.
   Der Helfer hängt am `d4y`-Netz (erreicht so den Store), die Volume-Daten fließen **nie** durch
   das Backend.
3. **Restore nur bei neuem/leerem Volume.** Beim Erzeugen eines Containers erkennt D4Y, ob das
   Named Volume **neu** angelegt wurde (vorher nicht existent); nur dann und nur bei aktivem Backup
   wird **vor dem App-Start** restauriert. Bestehende (befüllte) Volumes werden nie überschrieben.
4. **Cadence: periodisch.** Ein Scheduler sichert in einem konfigurierbaren Intervall
   (`d4y.backup.interval-ms`) die Volumes aller laufenden Apps mit aktivem Backup.
5. **Opt-in pro App:** `backup: true` im Desired-State-YAML. Ohne Backup (oder ohne konfigurierten
   Store) bleibt die App **ephemer** — klar erkennbar aus der Konfiguration/API.
6. **Geheimnisse:** S3-Credentials sind Backend-Konfiguration und werden **ausschließlich** als
   Env an die Helfer-Container übergeben — **nie** in Logs, API oder UI
   ([privacy-rules PR-5](../rules/privacy-rules.md)).

## Konsequenzen

- **Positiv:** Echter externer Store; erfüllt die Symmetrie „Daten ↔ Backup-Store". rclone deckt
  S3/MinIO/viele Kompatible ab. Kein tar/Protokoll-Eigenbau; Daten laufen nicht durch das Backend.
- **Positiv:** Restore-on-empty ist im Reconcile-Fluss verankert (frischer Server/Redeploy holt die
  Daten zurück).
- **Negativ:** Zusätzliches Helfer-Image (`rclone`) und ephemere Container je Operation.
- **Negativ:** Verzeichnis-Sync (kein Snapshot) ohne App-Quiescing — Konsistenz „crash-consistent";
  konsistente Snapshots/Quiescing sind eine spätere Ausbaustufe.
- **Negativ:** Ein einzelner, global konfigurierter Store (noch kein deklaratives
  Backup-Store-Domänenobjekt mit mehreren Zielen — folgt bei Bedarf).

## Alternativen

- **tar-Archiv + S3-Upload** — verworfen: bräuchte ein Image mit tar **und** S3-Client; rclone
  synct Verzeichnisse direkt.
- **Backup im Backend-Prozess (Daten durch die JVM streamen)** — verworfen: koppelt Datenpfad an
  das Backend, schlechter skalierbar, mehr Speicherlast.
- **Lokaler Volume-Store statt S3** — verworfen für diesen Schnitt: kein echter externer Store.
