# Domäne — Backup-Store

Ein **Backup-Store** ist das externe, vertrauenswürdige Ziel, in dem [Backups](backup.md) der
Anwendungsdaten abgelegt und aus dem sie wiederhergestellt werden.

## Begriff

Der Backup-Store ist zur Daten-Ebene das, was die [Registry](registry.md) zur Image-Ebene ist:
eine externe, vertrauenswürdige Quelle, aus der ein reproduzierbarer Zustand bezogen wird.
Backups können nicht im [Config-Repository](config-repository.md) liegen (zu groß, binär,
veränderlich) — daher ein eigenständiges externes Ziel.

Ein Backup-Store wird über das Config-Repository referenziert/konfiguriert und kann von mehreren
austauschbaren [Servern](server.md) gemeinsam genutzt werden. Dadurch kann ein frischer Server
die Daten einer App aus demselben Store wiederherstellen.

## Beziehungen

- Nimmt [Backups](backup.md) auf und liefert sie für Restores.
- Wird über das [Config-Repository](config-repository.md) referenziert.
- Symmetrisch zur [Registry](registry.md) (Images) — hier für Daten.

## Regeln

- Ein Backup-Store ist ein **externes** Ziel; Backups liegen **niemals** im Config-Repository.
- Es werden ausschließlich **vertrauenswürdige** Backup-Stores verwendet.
- Die Menge der Backup-Stores wird **deklarativ** im Config-Repository festgelegt.
- Backups können sensible Anwendungsdaten enthalten; ihr Schutz richtet sich nach den
  [Privacy-Rules](../rules/privacy-rules.md).
