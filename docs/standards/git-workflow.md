# Standard — Git-Workflow

> **Status: Draft.** Vorläufige Konventionen für die Arbeit an diesem Repository.

D4Y ist Git-nativ — Git ist nicht nur Versionsverwaltung, sondern das Betriebsmodell der
Plattform ([ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)). Ein sauberer,
nachvollziehbarer Git-Verlauf ist entsprechend wichtig.

## Branches

- `main` — stets konsistent; Doku und (später) Code widersprechen sich nicht.
- Arbeits-Branches pro Thema/Änderung; keine direkten unklaren Sammelcommits auf `main`.

## Commits

- Kleine, thematisch fokussierte Commits.
- Aussagekräftige Commit-Messages (Was und Warum).
- Doku-Änderungen und zugehörige (spätere) Code-Änderungen möglichst gemeinsam nachvollziehbar.

## Reviews

- Änderungen an Domäne, ADRs und Regeln werden reviewt, bevor sie `main` erreichen.
- Ein ADR wird erst nach Review und expliziter Freigabe auf `Accepted` gesetzt
  ([adr](adr.md)).

## Bezug zum Config-Repository

Dieses Repository enthält die **Plattform D4Y** selbst. Der **Sollzustand betriebener
Infrastruktur** liegt in einem **separaten** [Config-Repository](../domain/config-repository.md)
— nicht hier.
