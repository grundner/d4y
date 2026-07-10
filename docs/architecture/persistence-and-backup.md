# Architektur — Persistenz & Backup

Status: Draft
Bezug: [ADR-0009](../decisions/0009-persistence-optional-backup-restore.md),
[volume](../domain/volume.md), [backup](../domain/backup.md),
[backup-store](../domain/backup-store.md), [reconciliation-loop](reconciliation-loop.md)

Persistenz ist der Punkt, an dem die "Server sind austauschbar / aus Git wiederherstellbar"-
Prämisse ([ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)) unter Spannung gerät:
Zur Laufzeit erzeugte Daten liegen in [Volumes](../domain/volume.md), nicht in Git, und sind
nicht aus Git rekonstruierbar.

D4Y löst das über ein **optionales, aktives Backup/Restore pro App**. Damit entsteht dieselbe
Symmetrie wie beim Code: **Code** ist über [Images](../domain/container-image.md) aus einer
[Registry](../domain/registry.md) reproduzierbar, **Daten** über [Backups](../domain/backup.md)
aus einem [Backup-Store](../domain/backup-store.md).

## Zwei Arten von Zustand

| Art | Ort | Rekonstruierbar aus | Teil des Soll-Zustands |
| --- | --- | --- | --- |
| Reproduzierbarer Sollzustand | Config-Repo (Git) | Git | ja |
| Persistente Daten | Volume (Host/Engine) | Backup-Store (falls Backup an) | **nur die Deklaration**, nicht der Inhalt |

## Mounts

- **Named Volume** *(Standard)* — engine-verwaltet, klar sicherbar; Standard für persistente Daten.
- **Bind Mount** — nur für Host-Integration (Sockets, injizierte Config), nicht für persistente
  Anwendungsdaten. → [volume](../domain/volume.md)

## Ablauf (in den Reconciliation-Loop eingebettet)

```text
  Deploy / Platzierung einer App auf einem Server
        │
        ▼
  Volume vorhanden & befüllt? ──ja──► NICHT wiederherstellen (Live-Daten bleiben unangetastet)
        │ nein (leer/neu)
        ▼
  Backup für App konfiguriert? ──nein──► leer starten (App ist ephemer)
        │ ja
        ▼
  Restore aus letztem Backup (Backup-Store ──► Volume)
        │
        ▼
  App starten
        │
        ▼
  Laufzeit: aktives Backup der Volume-Daten ──► Backup-Store   (falls konfiguriert)
```

Beim **Replace/Redeploy** oder auf einem **frischen Server** ist das Volume leer/neu — die App
kommt (bei aktivem Backup) mit ihren Daten wieder hoch. Ohne Backup startet sie leer; der
Datenverlust ist ein **akzeptierter** Zustand.

## Invarianten

- **Restore nur bei leerem/neuem Volume** — bestehende Live-Daten werden nie überschrieben.
  → [backup](../domain/backup.md)
- **Deklaration ≠ Inhalt** — nur die Existenz/Konfiguration eines Volumes ist Sollzustand; der
  Inhalt ist es nicht. → [desired-vs-actual-state](../domain/desired-vs-actual-state.md)
- **Ohne Backup = ephemer** — explizit und aus der Konfiguration erkennbar.

## Einordnung in den Reconciliation-Loop

Der [Reconciliation-Loop](reconciliation-loop.md) stellt beim Herstellen des Sollzustands sicher,
dass deklarierte Volumes existieren und — bei leerem Volume und konfiguriertem Backup — vor dem
Start der App ein Restore erfolgt.

> Konkrete Backup-Cadence, Protokolle, Snapshot-Konsistenz (z. B. App-Quiescing) und Tooling sind
> **Implementierungsentscheidungen** und werden bei der Umsetzung per ADR festgelegt.

## Umsetzungsstand ([ADR-0020](../decisions/0020-backup-restore-s3-rclone.md))

- **Backup-Store:** S3-kompatibel (`d4y.backup.s3.*`; lokal via MinIO). Opt-in pro App mit
  `backup: true` im [Desired-State-YAML](desired-state-yaml.md).
- **Mechanismus:** kurzlebige **rclone**-Helfer-Container synchronisieren das Named Volume mit
  `<store>/<app>/<vol>` — Backup (Volume→Store) periodisch (`d4y.backup.interval-ms`), Restore
  (Store→Volume) **nur bei neu angelegtem Volume** vor dem App-Start. Die Daten laufen nie durch
  das Backend; S3-Credentials nur als Container-Env.
- **Offen:** konsistente Snapshots/App-Quiescing (aktuell crash-consistent), mehrere/deklarative
  Backup-Stores, Restore-Historie/Point-in-Time.
