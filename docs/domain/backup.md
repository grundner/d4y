# Domäne — Backup (Backup & Restore)

**Backup** ist der optionale Mechanismus, der die Daten eines [Volumes](volume.md) durable
macht und damit die Wiederherstellbarkeit einer [Application](application.md) auf einem frischen
Server ermöglicht. Backup verhält sich zu Daten wie das [Container-Image](container-image.md) zu
Code: es ist die externe, vertrauenswürdige Quelle, aus der ein reproduzierbarer Zustand
entsteht.

## Begriff

Ist für eine App ein Backup **konfiguriert**, so werden die Daten ihrer [Volumes](volume.md)
**aktiv** in einen externen [Backup-Store](backup-store.md) gesichert. Beim Deployment kann eine
App aus diesem Backup **wiederhergestellt** werden.

Backup ist **optional pro App**:

- **aktiviert** → Daten sind durable; ein frischer/leerer Volume-Zustand wird aus dem letzten
  Backup wiederhergestellt. Server bleiben austauschbar, ohne Daten zu verlieren.
- **deaktiviert** → die App ist **ephemer**; zur Laufzeit erzeugte Daten gehen bei
  Redeploy/Serververlust verloren. Dies ist ein **explizit akzeptierter** Zustand.

## Restore — nur bei leerem/neuem Volume

Ein Restore erfolgt **ausschließlich**, wenn das Ziel-Volume **leer bzw. neu** ist (frischer
Server, Erstplatzierung der App). Ein Restore **überschreibt niemals** bestehende Live-Daten.
Dadurch kann ein automatischer Restore keinen Datenverlust auf laufenden, bereits befüllten
Volumes verursachen.

## Beziehungen

- Sichert die Daten von [Volumes](volume.md) einer [Application](application.md).
- Ziel der Sicherung ist der [Backup-Store](backup-store.md).
- Ermöglicht die Austauschbarkeit von [Servern](server.md) trotz persistenter Daten.

## Regeln

- Backup ist **optional pro App** und wird deklarativ im [Config-Repository](config-repository.md)
  konfiguriert.
- Ein **Restore** erfolgt **nur bei leerem/neuem Volume** und überschreibt **niemals** bestehende
  Live-Daten.
- Ist kein Backup konfiguriert, sind die Daten der App **ephemer**; ihr Verlust ist akzeptiert
  und muss aus der Konfiguration klar erkennbar sein.
- Backups werden ausschließlich in einem vertrauenswürdigen [Backup-Store](backup-store.md)
  abgelegt.
