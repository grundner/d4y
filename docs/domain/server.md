# Domäne — Server

Ein **Server** ist eine Maschine, auf der Anwendungen als Container ausgeführt werden.
Innerhalb von D4Y ist ein Server bewusst **austauschbar** und **zustandslos**.

## Begriff

Ein Server hält **keine individuelle Konfiguration**. Sein gesamter Sollzustand ergibt sich
aus dem [Config-Repository](config-repository.md); sein Ist-Zustand aus den aktuell laufenden
Containern.

Eine frisch installierte Maschine kann ihre vollständige Laufzeitumgebung allein aus dem
Config-Repository wiederherstellen — der Server selbst trägt keine einzigartige Information,
deren Verlust nicht rekonstruierbar wäre.

## Persistente Daten und Austauschbarkeit

Zur Laufzeit erzeugte Daten liegen in [Volumes](volume.md) und sind **nicht** aus dem
Config-Repository rekonstruierbar. Die Austauschbarkeit eines Servers bleibt dennoch erhalten:

- Für Apps **mit** [Backup](backup.md) kommt der durable Datenbestand auf einem frischen Server
  per Restore aus dem [Backup-Store](backup-store.md) zurück.
- Für Apps **ohne** Backup sind die Daten **ephemer**; ihr Verlust bei Serverwechsel ist ein
  akzeptierter Zustand.

Ein Server trägt also weiterhin **keine** einzigartige, nicht rekonstruierbare Konfiguration;
persistente Daten werden über Backups, nicht über den Server selbst, durable gemacht.

## Beziehungen

- Führt [Applications](application.md) als Container aus.
- Hält deren [Volumes](volume.md); deren Durability ergibt sich aus [Backup](backup.md).
- Sein Ist-Zustand fließt in den [Desired-vs-Actual-State](desired-vs-actual-state.md) ein.

## Regeln

- Ein Server enthält **keine** individuelle, nur lokal vorhandene Konfiguration.
- Ein Server ist **austauschbar**: geht er verloren, wird die Laufzeitumgebung aus dem
  Config-Repository und — für Apps mit Backup — der Datenbestand aus dem [Backup-Store](backup-store.md)
  auf einer neuen Maschine wiederhergestellt.
- Der Sollzustand eines Servers wird **niemals** direkt auf der Maschine, sondern ausschließlich
  im Config-Repository geändert.
- Ein Server hält keine persistente Information, die nicht entweder aus dem Config-Repository
  oder aus einem [Backup](backup.md) wiederherstellbar ist.
- Die externe Erreichbarkeit von Anwendungen hängt **nicht** von der IP eines konkreten Servers
  ab, sondern von [Routes](route.md) und einem stabilen Eintrittspunkt bzw. D4Y-managed
  [DNS](dns-provider.md). → [ADR-0010](../decisions/0010-dns-ingress-service-discovery.md)
