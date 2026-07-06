# Domäne — Reconciliation

**Reconciliation** ist der fachliche Vorgang, den [Ist-Zustand](desired-vs-actual-state.md)
kontinuierlich an den [Soll-Zustand](desired-vs-actual-state.md) anzugleichen.

## Begriff

Reconciliation beobachtet den tatsächlichen Zustand, vergleicht ihn mit dem gewünschten
Zustand aus dem [Config-Repository](config-repository.md) und stellt bei Abweichungen den
Soll-Zustand wieder her. Fehler — etwa abgestürzte oder abweichende Container — werden dabei
automatisch erkannt und korrigiert. Dadurch ist die Infrastruktur **selbstheilend**.

Reconciliation ist **kontinuierlich** und **wiederholbar**: Ist Soll gleich Ist, verändert ein
weiterer Durchlauf nichts.

## Zustände (fachlich)

- **In Sync** — Ist entspricht Soll; keine Aktion nötig.
- **Drift erkannt** — Abweichung zwischen Soll und Ist festgestellt.
- **Reconciling** — Angleichung an den Soll-Zustand läuft.
- **Gehalten (Hold)** — für dieses Ziel ist ein [Reconciliation-Hold](reconciliation-hold.md)
  aktiv; die Angleichung ist zeitlich begrenzt ausgesetzt.
- **Fehler** — Angleichung war (temporär) nicht möglich; wird erneut versucht.

## Beziehungen

- Liest den Soll aus dem [Config-Repository](config-repository.md).
- Beobachtet den Ist auf den [Servern](server.md).
- Der technische Ablauf ist in [../architecture/reconciliation-loop.md](../architecture/reconciliation-loop.md)
  beschrieben.

## Regeln

- Reconciliation stellt bei Abweichung stets den **Soll-Zustand** her, nicht umgekehrt.
- Erkannte Fehler werden **automatisch** korrigiert, ohne manuelle Eingriffe (Self-Healing).
- Reconciliation ist **idempotent**: bei Übereinstimmung von Soll und Ist erfolgt keine Änderung.
- Ein temporär fehlgeschlagener Angleich führt zu einem erneuten Versuch, nicht zu einem
  dauerhaften Fehlerzustand.
- Für ein Ziel mit aktivem [Reconciliation-Hold](reconciliation-hold.md) wird die Angleichung
  **übersprungen**, bis der Hold abläuft; andere Ziele bleiben davon unberührt.
