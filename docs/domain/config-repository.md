# Domäne — Config-Repository

Das **Config-Repository** ist das Git-Repository, das den **Sollzustand** der gesamten
Infrastruktur deklarativ beschreibt. Es ist die einzige Quelle der Wahrheit für alles, was
auf der Plattform laufen soll.

## Begriff

Ein Config-Repository beschreibt vollständig und deklarativ:

- welche Anwendungen laufen sollen ([Application](application.md)),
- aus welchen [Container-Images](container-image.md) sie stammen,
- aus welchen [Registries](registry.md) diese Images bezogen werden.

Das Repository enthält den gewünschten Zustand — nicht die Schritte, um ihn zu erreichen.

## Beziehungen

- Liefert den **Soll-Zustand** für den [Desired-vs-Actual-State](desired-vs-actual-state.md).
- Wird vom [Reconciliation](reconciliation.md)-Prozess kontinuierlich gelesen.
- Die konkrete **YAML-Struktur** des Sollzustands (interim aus einem lokalen Verzeichnis,
  [ADR-0011](../decisions/0011-interim-local-desired-state-source.md)) ist in
  [architecture/desired-state-yaml](../architecture/desired-state-yaml.md) beschrieben.

## Regeln

- Das Config-Repository ist die **einzige** Quelle für Infrastrukturänderungen.
- Jede gewünschte Änderung an der Infrastruktur **muss** als Änderung im Config-Repository
  ausgedrückt werden.
- Der beschriebene Zustand ist rein **deklarativ** (Soll), nicht imperativ (Schritte).
- Der Sollzustand ist **versioniert** — jede Änderung ist über die Git-Historie nachvollziehbar.
