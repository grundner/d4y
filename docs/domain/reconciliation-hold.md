# Domäne — Reconciliation-Hold

Ein **Reconciliation-Hold** ist eine **zeitlich begrenzte** Aussetzung der
[Reconciliation](reconciliation.md) für ein bestimmtes Ziel (z. B. eine
[Application](application.md)). Er ermöglicht [operative Aktionen](operational-action.md) wie
temporäre Parameter oder einen haltbaren Stop, ohne dass Self-Healing sie sofort zurücksetzt.

## Begriff

Ohne Hold gilt reines GitOps: Jede Abweichung vom Sollzustand wird beim nächsten Durchlauf
korrigiert. Ein Hold **pausiert** diese Korrektur für sein Ziel für eine begrenzte Dauer. Nach
**automatischem Ablauf** greift wieder reines GitOps/Self-Healing.

Der Hold ist selbst **kein Sollzustand**, sondern **operativer** Zustand: Er wird nicht in Git
beschrieben, ist transient und läuft von selbst ab. Dadurch bleibt garantiert, dass die
Plattform in einen selbstheilenden Zustand zurückkehrt.

## Lebenszyklus

```text
  angefordert ──► aktiv (Reconciliation für Ziel pausiert) ──► abgelaufen ──► GitOps aktiv
                       │
                       └─ vorzeitig freigegeben ──────────────► GitOps aktiv
```

## Beziehungen

- Bezieht sich auf ein Ziel, meist eine [Application](application.md).
- Ermöglicht bestimmte [operative Aktionen](operational-action.md).
- Beeinflusst die [Reconciliation](reconciliation.md): das Ziel wird während des Holds übersprungen.

## Regeln

- Ein Hold ist **immer zeitlich begrenzt** und läuft **automatisch** ab; ein unbefristeter Hold
  ist nicht zulässig. → [ADR-0012](../decisions/0012-operational-actions-and-reconciliation-hold.md)
- Während eines aktiven Holds wird das Ziel von der Reconciliation **übersprungen**, nicht global
  abgeschaltet — andere Ziele bleiben selbstheilend.
- Nach Ablauf oder Freigabe kehrt das Ziel **automatisch** zu reinem GitOps/Self-Healing zurück.
- Ein Hold ist **operativer**, nicht deklarativer Zustand; er wird **nicht** im Config-Repository
  beschrieben.
- Beginn, Dauer und Ende eines Holds werden **auditiert** und **sichtbar** gemacht.
