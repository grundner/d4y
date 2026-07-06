# Business Rules — übergreifend

Dieses Dokument enthält **ausschließlich übergreifende** Geschäftsregeln, die nicht einem
einzelnen Domänenobjekt gehören. Objektspezifische Regeln stehen in der `## Regeln`-Sektion
des jeweiligen Dokuments in [`../domain/`](../domain/).

## BR-1 — Sollzustand-Änderungen nur über das Config-Repository

Jede Änderung am **deklarativen Sollzustand** (welche Apps, Images, Routes, Volumes) erfolgt
ausschließlich über das [Config-Repository](../domain/config-repository.md). API und UI dürfen
den Sollzustand **niemals** ändern. → [ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)

Davon unberührt sind [operative Aktionen](../domain/operational-action.md) (Restart, Debugging,
temporäre Parameter), die den Sollzustand nicht verändern — siehe BR-11/BR-12.

## BR-2 — Kein Build auf dem Zielsystem

Anwendungen werden niemals auf dem Zielsystem gebaut, sondern ausschließlich als
unveränderliche Images bereitgestellt. → [ADR-0002](../decisions/0002-immutable-images-no-build-on-target.md)

## BR-3 — Nur vertrauenswürdige Registries

Es werden ausschließlich Images aus vertrauenswürdigen [Registries](../domain/registry.md)
bezogen.

## BR-4 — Soll hat Vorrang

Bei Abweichung zwischen Soll und Ist gilt stets der Soll-Zustand aus dem Config-Repository als
korrekt; der Ist-Zustand wird angeglichen, niemals umgekehrt.
→ [desired-vs-actual-state](../domain/desired-vs-actual-state.md)

## BR-5 — Zustandslose, austauschbare Server

Server tragen keine individuelle Konfiguration; ihr Zustand ist jederzeit allein aus dem
Config-Repository wiederherstellbar. → [server](../domain/server.md)

## BR-6 — Automatische Fehlerkorrektur

Erkannte Abweichungen und Fehler werden automatisch und ohne manuelle Eingriffe korrigiert
(Self-Healing). → [ADR-0007](../decisions/0007-continuous-reconciliation-self-healing.md)

## BR-7 — Persistente Daten außerhalb von Git; Durability nur über Backup

Persistente Daten liegen in [Volumes](../domain/volume.md), nicht im Config-Repository, und sind
nicht aus Git rekonstruierbar. Datendurability entsteht ausschließlich über ein **optionales**
[Backup](../domain/backup.md) in einen [Backup-Store](../domain/backup-store.md). Ist kein Backup
konfiguriert, ist die App **ephemer**; der Verlust ihrer Laufzeitdaten ist akzeptiert und muss aus
der Konfiguration klar erkennbar sein. → [ADR-0009](../decisions/0009-persistence-optional-backup-restore.md)

## BR-8 — Restore überschreibt keine Live-Daten

Ein [Restore](../domain/backup.md) aus einem Backup erfolgt **ausschließlich** bei leerem/neuem
Volume und überschreibt **niemals** bestehende Live-Daten.

## BR-9 — Ingress und DNS sind deklarativ

[Routes](../domain/route.md) (Ingress) und die DNS-Verwaltung werden ausschließlich deklarativ
im [Config-Repository](../domain/config-repository.md) beschrieben; Änderungen erfolgen nur dort.
→ [ADR-0010](../decisions/0010-dns-ingress-service-discovery.md)

## BR-10 — Keine Abhängigkeit von einer konkreten Server-IP

Öffentliche und interne Erreichbarkeit hängen **nie** an der IP eines konkreten
[Servers](../domain/server.md). Öffentlicher Zugriff erfolgt über einen stabilen Eintrittspunkt
**oder** D4Y-managed [DNS](../domain/dns-provider.md); interne Adressierung über stabile Namen
([Service-Discovery](../domain/service-discovery.md)).

## BR-11 — Operative Aktionen sind transient und auditiert

[Operative Aktionen](../domain/operational-action.md) über UI/API (Inspizieren/Debuggen,
Lifecycle-Nudges, temporäre Parameter) ändern **nie** den Sollzustand. Sie sind **transient**,
werden **auditiert** und als sanktionierte, temporäre Drift **sichtbar** gemacht.
→ [ADR-0012](../decisions/0012-operational-actions-and-reconciliation-hold.md)

## BR-12 — Reconciliation-Hold ist immer zeitlich begrenzt

Ein [Reconciliation-Hold](../domain/reconciliation-hold.md) pausiert die Reconciliation für ein
Ziel nur **zeitlich begrenzt** und läuft **automatisch** ab. Ein unbefristeter Hold ist
unzulässig; nach Ablauf kehrt das Ziel zu reinem GitOps/Self-Healing zurück.
