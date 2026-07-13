# Domäne — Application

> **Geändert durch [ADR-0029](../decisions/0029-docker-compose-single-source-format.md):** Eine
> Application ist nun ein **Docker-Compose-Projekt** (je App ein Verzeichnis mit `compose.yaml`) und
> kann **mehrere Services/Container** umfassen. Images werden bezogen **oder** auf dem Ziel gebaut
> (`build:`). Die frühere „genau ein Image/Container pro App"-Regel gilt nicht mehr.

Eine **Application** ist eine containerisierte Anwendung, die auf der Plattform laufen soll.
Sie wird im [Config-Repository](config-repository.md) deklarativ beschrieben.

## Begriff

Eine Application beschreibt, **was** laufen soll — nicht, wie es gebaut wird. Sie referenziert
ein unveränderliches [Container-Image](container-image.md) aus einer vertrauenswürdigen
[Registry](registry.md) und beschreibt den gewünschten Laufzeitzustand (z. B. gewünschte
Instanzen, Konfigurationswerte).

Anwendungen werden **niemals auf dem Zielsystem gebaut**, sondern ausschließlich aus fertigen
Images bereitgestellt.

Eine Application kann zusätzlich **[Volumes](volume.md)** deklarieren, in denen sie zur Laufzeit
Daten ablegt, sowie **optional** eine **[Backup](backup.md)-Policy**, die diese Daten durable und
über einen frischen Server wiederherstellbar macht. Ohne Backup-Policy ist die App ephemer.

Konfigurationswerte werden als deklarierte **Umgebungsvariablen** (`env`) beschrieben; sie sind
Teil des Sollzustands. Operative Aktionen können sie temporär überschreiben (transiente,
sanktionierte Drift unter einem [Hold](reconciliation-hold.md)).

Für die Erreichbarkeit besitzt jede App einen stabilen internen Namen
([Service-Discovery](service-discovery.md)) und kann **optional** eine oder mehrere
**[Routes](route.md)** (Hostname → App) für externen Zugriff deklarieren.

## Beziehungen

- Referenziert genau ein [Container-Image](container-image.md).
- Wird auf einem oder mehreren [Servern](server.md) ausgeführt.
- Kann [Volumes](volume.md) und optional eine [Backup](backup.md)-Policy deklarieren.
- Besitzt einen stabilen internen Namen ([Service-Discovery](service-discovery.md)) und kann
  optional [Routes](route.md) für externen Ingress deklarieren.
- Ihr Soll ist Teil des [Desired-vs-Actual-State](desired-vs-actual-state.md).

## Regeln

- Eine Application wird **deklarativ** im Config-Repository beschrieben.
- Eine Application wird **niemals auf dem Zielsystem gebaut** — nur aus einem fertigen,
  unveränderlichen Image bereitgestellt.
- Eine Application referenziert ausschließlich Images aus **vertrauenswürdigen** Registries.
- Der gewünschte Laufzeitzustand einer Application ergibt sich allein aus ihrer Beschreibung
  im Config-Repository.
- [Volumes](volume.md) und die optionale [Backup](backup.md)-Policy einer Application werden
  **deklarativ** im Config-Repository beschrieben. Ist keine Backup-Policy gesetzt, gilt die App
  als **ephemer** — der Verlust ihrer Laufzeitdaten ist akzeptiert.
- [Routes](route.md) einer Application werden **deklarativ** im Config-Repository beschrieben.
  Interne Adressierung erfolgt über den stabilen Namen der App
  ([Service-Discovery](service-discovery.md)), nicht über konkrete Server-IPs.
