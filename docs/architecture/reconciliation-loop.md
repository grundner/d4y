# Architektur — Reconciliation-Loop

Status: Draft
Bezug: [ADR-0007](../decisions/0007-continuous-reconciliation-self-healing.md),
[reconciliation](../domain/reconciliation.md),
[desired-vs-actual-state](../domain/desired-vs-actual-state.md)

Der Reconciliation-Loop ist das Herzstück von D4Y. Er sorgt dafür, dass der **Ist-Zustand**
der Maschine kontinuierlich dem **Soll-Zustand** aus dem Git-Config-Repo entspricht.

## Ablauf

```text
   ┌──────────────┐
   │  observe     │  Ist-Zustand von der Engine erfassen
   └──────┬───────┘
          ▼
   ┌──────────────┐
   │  load desired│  Sollzustand aus Git-Config-Repo lesen
   └──────┬───────┘
          ▼
   ┌──────────────┐
   │  diff        │  Abweichungen (Drift) ermitteln
   └──────┬───────┘
          ▼
   ┌──────────────┐
   │  reconcile   │  Aktionen ableiten und ausführen (start/stop/replace)
   └──────┬───────┘
          ▼
   ┌──────────────┐
   │  report      │  Ergebnis + Zustand für Frontend/API bereitstellen
   └──────┬───────┘
          ▼
        (warten / Intervall) ──────► zurück zu observe
```

## Eigenschaften

- **Kontinuierlich** — der Loop läuft periodisch bzw. ereignisgesteuert, nicht nur einmalig.
- **Idempotent** — mehrfaches Ausführen bei unverändertem Sollzustand ändert nichts.
- **Self-Healing** — erkannte Abweichungen (abgestürzte Container, Drift) werden automatisch
  korrigiert, ohne manuelle Eingriffe. → [reconciliation](../domain/reconciliation.md)
- **Deklarativ** — der Loop kennt nur Soll und Ist, keine imperativen Migrationsschritte.
- **Persistenz-bewusst** — beim Herstellen einer App stellt der Loop sicher, dass deklarierte
  [Volumes](../domain/volume.md) existieren und — bei **leerem/neuem** Volume und konfiguriertem
  [Backup](../domain/backup.md) — vor dem Start ein Restore erfolgt. Bestehende Live-Daten werden
  nie überschrieben. → [persistence-and-backup](persistence-and-backup.md)
- **Netzwerk-bewusst** — der Loop gleicht die Reverse-Proxy-Konfiguration mit den deklarierten
  [Routes](../domain/route.md) ab und hält — im **managed-Modus** — die DNS-Records beim
  [DNS-Provider](../domain/dns-provider.md) mit dem Soll konsistent.
  → [networking-and-dns](networking-and-dns.md)
- **Hold-bewusst** — Ziele mit aktivem [Reconciliation-Hold](../domain/reconciliation-hold.md)
  werden übersprungen (nicht global pausiert) und nach Ablauf des Holds wieder normal abgeglichen.
  → [operational-actions](operational-actions.md)

## Fehlerbehandlung (Prinzipien)

- Ein fehlgeschlagener Reconcile-Schritt darf den Loop nicht dauerhaft blockieren.
- Nicht erreichbare Registries/Repos führen zu Zurückstellung, nicht zu Zustandsverlust.
- Der zuletzt bekannte Zustand bleibt für Diagnose und Visualisierung sichtbar.

> Intervalle, Nebenläufigkeit, Backoff-Strategien und konkrete Aktionsableitung sind
> **Implementierungsentscheidungen** und werden erst bei der Umsetzung festgelegt.

Siehe auch: [container-backend-abstraction.md](container-backend-abstraction.md).
