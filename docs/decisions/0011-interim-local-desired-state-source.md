# ADR-0011: Interim — lokale Desired-State-Quelle vor Git-Anbindung

Status: Proposed
Datum: 2026-07-06
Betrifft: [config-repository](../domain/config-repository.md), [desired-vs-actual-state](../domain/desired-vs-actual-state.md), [ADR-0001](0001-git-as-single-source-of-truth.md)

> Format und Verarbeitung der lokalen YAML-Dateien sind in
> [architecture/desired-state-yaml](../architecture/desired-state-yaml.md) beschrieben.

## Kontext

[ADR-0001](0001-git-as-single-source-of-truth.md) (Accepted) legt fest, dass der Sollzustand
ausschließlich aus einem **Git-Config-Repository** stammt. Der erste Implementierungsschnitt
(Backend-Walking-Skeleton des Reconciliation-Loops) soll jedoch bewusst schmal bleiben und
zunächst **ohne** Git-Anbindung (Clone/Pull, Credentials, Pull-Zyklus) auskommen, um den Kern
— observe → diff → reconcile — isoliert lauffähig und testbar zu machen.

Das Lesen des Sollzustands aus einem **lokalen Verzeichnis** ist damit eine bewusste,
temporäre Abweichung von ADR-0001, die hier ehrlich dokumentiert wird (statt sie still zu
begehen — siehe [ADR-Prozess](../standards/adr.md)).

## Entscheidung

Für die erste Ausbaustufe liest D4Y den Sollzustand aus einem **lokalen Verzeichnis**
(`desired/`, YAML). Dieses Verzeichnis wird konzeptionell als lokaler Arbeitsstand des
Config-Repositories behandelt. Nur die **Bezugsquelle** ist interimistisch lokal statt aus Git
geklont; das deklarative Modell wächst regulär mit den Domänenschritten. Neben `name`/`image`
trägt das Interim-YAML inzwischen auch **Volume-Deklarationen** (Named Volumes, `name` + Mount-`path`)
gemäß [volume](../domain/volume.md) und [ADR-0009](0009-persistence-optional-backup-restore.md).

**Sunset:** Diese Entscheidung wird zurückgezogen (`Superseded`), sobald die Git-Anbindung
(Clone/Pull des Config-Repositories) implementiert ist. Ab dann gilt wieder ausschließlich
ADR-0001.

## Konsequenzen

- **Positiv:** Der Reconciliation-Loop ist früh isoliert lauf- und testbar, ohne Git-,
  Credential- und Netzwerk-Komplexität.
- **Negativ:** Vorübergehend ist der Sollzustand nicht versioniert/aus Git bezogen — die
  Kern-Garantie von ADR-0001 gilt in diesem Interim nur eingeschränkt.
- **Begrenzt:** Die Abweichung ist bewusst eng (nur die Bezugsquelle) und zeitlich befristet.

## Alternativen

- **Git-Anbindung sofort** — verworfen für den ersten Schnitt: vergrößert Umfang und Risiko,
  ohne den Kern (Reconcile) früher zu beweisen.
- **Stille Abweichung ohne ADR** — verworfen: widerspricht dem ADR-Prozess (keine stillen
  Verstöße gegen Accepted ADRs).
