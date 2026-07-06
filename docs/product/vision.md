# Vision

> **Hinweis:** Dieses Dokument gehört zu `docs/product/` und ist gemäß `CLAUDE.md`
> **sekundäres Wissen**. Es beschreibt Produktvision und Absicht, ist aber **kein
> Implementierungstreiber**. Verbindliche Vorgaben entstehen ausschließlich aus
> `docs/domain/` und **Accepted** ADRs in `docs/decisions/`.

## Git-native Runtime Platform

Die Vision ist es, eine **Git-first, Git-native Runtime Platform** für containerisierte
Anwendungen zu schaffen.

Jeder Aspekt der Infrastruktur wird deklarativ in Git beschrieben. Server selbst sind
austauschbar und enthalten keine individuelle Konfiguration. Eine frisch installierte
Maschine soll ihre vollständige Laufzeitumgebung allein durch einen Bootstrap-Befehl und
ein Konfigurations-Repository automatisch wiederherstellen können.

Anwendungen werden niemals auf dem Zielsystem gebaut. Stattdessen werden sie ausschließlich
als unveränderliche Container-Images aus vertrauenswürdigen Registries bereitgestellt.
Dadurch entstehen deterministische und reproduzierbare Deployments über alle Umgebungen
hinweg.

Die Plattform gleicht den tatsächlichen Zustand der Infrastruktur kontinuierlich mit dem in
Git definierten Sollzustand ab. Fehler werden automatisch erkannt, korrigiert und der
gewünschte Zustand ohne manuelle Eingriffe wiederhergestellt. Die Infrastruktur bleibt
dadurch dauerhaft selbstheilend.

D4Y versteht sich nicht als weiterer Container-Orchestrator, sondern als schlanke
Runtime-Schicht, die Git zum zentralen Betriebsmodell der Infrastruktur macht. Anstatt Server
manuell zu administrieren, wird die gesamte Infrastruktur deklarativ beschrieben und von der
Plattform kontinuierlich umgesetzt.

**Das Ziel ist einfach: Infrastruktur soll genauso reproduzierbar, versionierbar und
deterministisch werden wie Software selbst.**

## Architektur

D4Y ist eine Java-basierte Anwendung, die als Runtime-Layer Container-Engines steuert. Die
erste Implementierung verwendet Docker als Laufzeitumgebung. Die Architektur wird jedoch von
Beginn an so abstrahiert, dass zukünftig auch andere Container-Backends ohne grundlegende
Änderungen unterstützt werden können.

Die Installation soll auf einer frisch installierten Linux-Maschine über einen einzigen
Bootstrap-Befehl erfolgen. Während der Inbetriebnahme werden lediglich das
Git-Konfigurationsrepository sowie die erforderlichen Zugangsdaten abgefragt. Anschließend
übernimmt D4Y den gesamten Einrichtungsprozess selbstständig – vom Abrufen der Konfiguration
über die Bereitstellung der Infrastruktur bis hin zum Start aller Anwendungen.

D4Y besteht aus einem Backend auf Basis von Java 21 und Spring Boot sowie einem Frontend auf
Basis von Next.js und React. Das Frontend dient zunächst ausschließlich der Statuskontrolle
und Visualisierung des aktuellen Plattformzustands. Sämtliche Änderungen an der Infrastruktur
erfolgen ausschließlich über das Git-Konfigurationsrepository.

Backend und Frontend werden in der ersten Ausbaustufe gemeinsam als einzelnes Container-Image
ausgeliefert. Dadurch bleibt die Bereitstellung möglichst einfach und reproduzierbar. Eine
spätere Aufteilung in mehrere Komponenten bleibt ausdrücklich möglich.

## Leitprinzipien

1. **Git ist die einzige Quelle der Wahrheit.** → [ADR-0001](../decisions/0001-git-as-single-source-of-truth.md)
2. **Unveränderliche Images, kein Build auf dem Zielsystem.** → [ADR-0002](../decisions/0002-immutable-images-no-build-on-target.md)
3. **Kontinuierliche Reconciliation, Self-Healing.** → [ADR-0007](../decisions/0007-continuous-reconciliation-self-healing.md)
4. **Austauschbare, zustandslose Server.** → [server](../domain/server.md)
5. **Backend-Abstraktion über Container-Engines, Docker zuerst.** → [ADR-0005](../decisions/0005-container-backend-abstraction-docker-first.md)
