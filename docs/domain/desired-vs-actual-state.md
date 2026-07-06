# Domäne — Desired-vs-Actual-State (Soll/Ist)

Das Begriffspaar **Soll-Zustand** und **Ist-Zustand** ist das zentrale Domänenkonzept von D4Y.

## Begriff

- **Soll-Zustand (Desired State):** der im [Config-Repository](config-repository.md)
  deklarativ beschriebene Zustand — welche [Applications](application.md) aus welchen
  [Images](container-image.md) laufen sollen.
- **Ist-Zustand (Actual State):** der tatsächlich auf den [Servern](server.md) beobachtete
  Zustand der laufenden Container.

Weichen Soll und Ist voneinander ab, spricht man von **Drift**. Die Aufgabe der Plattform ist,
Drift zu erkennen und aufzulösen — das leistet die [Reconciliation](reconciliation.md).

**Persistente Daten sind nicht Teil des Soll-Zustands.** Zum Soll gehört nur die **Deklaration**
eines [Volumes](volume.md) (dass und wo es existiert), **nicht** dessen **Inhalt**. Datendurability
wird nicht über Git, sondern über [Backups](backup.md) hergestellt.

## Beziehungen

- Der Soll-Zustand stammt aus dem [Config-Repository](config-repository.md).
- Der Ist-Zustand stammt von den [Servern](server.md).
- Der Abgleich beider ist Gegenstand der [Reconciliation](reconciliation.md).

## Regeln

- Der **Soll-Zustand** ist immer der im Config-Repository beschriebene Zustand — er hat Vorrang.
- Bei **Drift** gilt der Soll-Zustand als korrekt; der Ist-Zustand wird an ihn angeglichen.
- Der Ist-Zustand wird niemals als neue Wahrheit in den Soll-Zustand zurückgeschrieben.
- Der **Inhalt** persistenter [Volumes](volume.md) ist **nicht** Teil des Soll-Zustands; nur
  ihre Deklaration ist es. Durability erfolgt über [Backup](backup.md), nicht über Git.
