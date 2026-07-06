# ADR-0008: Inbetriebnahme über einen einzigen Bootstrap-Befehl

Status: Proposed
Datum: 2026-07-06
Betrifft: [architecture/bootstrap](../architecture/bootstrap.md), [server](../domain/server.md), [config-repository](../domain/config-repository.md)

## Kontext

Eine frisch installierte Linux-Maschine soll ihre vollständige Laufzeitumgebung ohne
aufwändiges manuelles Setup wiederherstellen können. Da Server zustandslos und austauschbar
sind ([ADR-0001](0001-git-as-single-source-of-truth.md)), darf die Inbetriebnahme keine
individuelle Server-Konfiguration erfordern.

## Entscheidung

Die Installation erfolgt über **einen einzigen Bootstrap-Befehl**. Während der Inbetriebnahme
werden **ausschließlich** das Git-Config-Repository und die erforderlichen Zugangsdaten
abgefragt. D4Y übernimmt danach den gesamten Einrichtungsprozess selbstständig — vom Abrufen
der Konfiguration über die Bereitstellung der Infrastruktur bis zum Start aller Anwendungen.

## Konsequenzen

- **Positiv:** Minimaler manueller Aufwand, reproduzierbare Inbetriebnahme.
- **Positiv:** Austauschbare Server — eine neue Maschine ist schnell einsatzbereit.
- **Negativ:** Bootstrap-Mechanismus und Credential-Handling müssen robust und sicher sein
  (siehe [privacy-rules](../rules/privacy-rules.md)).

## Alternativen

- Mehrstufiges manuelles Setup — verworfen (nicht reproduzierbar, fehleranfällig).
- Vorkonfigurierte, individuelle Server-Images — verworfen (widerspricht Zustandslosigkeit).
