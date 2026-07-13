# ADR-0009: Persistenz über optionales Backup/Restore

Status: Proposed
Datum: 2026-07-06
Betrifft: [volume](../domain/volume.md), [backup](../domain/backup.md), [backup-store](../domain/backup-store.md), [server](../domain/server.md), [architecture/persistence-and-backup](../architecture/persistence-and-backup.md)

> **Ergänzt durch [ADR-0029](0029-docker-compose-single-source-format.md):** Volumes werden nun in
> **Docker Compose** deklariert (Compose-Syntax, inkl. Bind-Mounts, wo sinnvoll) — die strikte
> „nur Named Volumes"-Vorgabe entfällt. Die Backup-Semantik (opt-in, Restore nur in leeres Volume)
> bleibt; das Opt-in steht in der Sidecar-`d4y.yaml`, Discovery über die Compose-Volume-Namen.

## Kontext

D4Y-Server sind austauschbar und zustandslos; eine frische Maschine soll ihre Laufzeitumgebung
allein aus Git wiederherstellen ([ADR-0001](0001-git-as-single-source-of-truth.md),
[server](../domain/server.md)). Persistente Anwendungsdaten widersprechen dem zunächst: Sie
liegen zur Laufzeit in Volumes, nicht in Git, und sind nicht aus Git rekonstruierbar. Es braucht
einen Mechanismus, der Daten-Durability herstellt, ohne die Austauschbarkeit der Server aufzugeben.

## Entscheidung

Persistenz wird über einen **optionalen, aktiven Backup/Restore-Mechanismus pro App** gelöst:

1. **Mounts werden deklariert**, nicht ihr Inhalt. **Named Volumes** sind der Standard für
   persistente Daten; **Bind Mounts** sind ausschließlich für Host-Integration zulässig.
2. **Backup ist optional pro App.** Ist es aktiviert, werden die Volume-Daten aktiv in einen
   externen [Backup-Store](../domain/backup-store.md) gesichert (symmetrisch zur
   [Registry](../domain/registry.md) für Images).
3. **Restore erfolgt nur bei leerem/neuem Volume** (frischer Server, Erstplatzierung) und
   überschreibt **niemals** bestehende Live-Daten.
4. **Ohne Backup ist die App ephemer:** zur Laufzeit erzeugte Daten gehen bei Redeploy/
   Serververlust verloren. Dies ist ein **explizit akzeptierter** Zustand, kein Fehler.

## Konsequenzen

- **Positiv:** Server bleiben austauschbar — durable Daten kommen via Restore auf einem frischen
  Server zurück. Symmetrie zu Images/Registry bleibt Git-native gewahrt.
- **Positiv:** Restore-auf-leeres-Volume verhindert versehentlichen Überschreib von Live-Daten.
- **Positiv:** Ephemere Apps sind ausdrücklich möglich und klar als solche erkennbar.
- **Negativ:** Datendurability hängt an einem korrekt konfigurierten, vertrauenswürdigen
  Backup-Store; ohne Backup ist Datenverlust systemimmanent (bewusst gewählt).
- **Negativ:** Konsistente Backups laufender Apps (Quiescing/Snapshots) erfordern sorgfältiges
  Implementierungsdesign.

## Alternativen

- **Externes/repliziertes Storage als Pflicht** (Server strikt zustandslos) — verworfen: höhere
  Betriebsanforderung, weniger flexibel als optionales Backup.
- **Daten in Git** — verworfen: ungeeignet für große, binäre, veränderliche Daten.
- **Restore immer beim Deploy** — verworfen: würde neuere Live-Daten überschreiben (Datenverlust).
- **Keine Persistenzunterstützung** — verworfen: reale Apps benötigen persistente Daten.
