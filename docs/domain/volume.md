# Domäne — Volume (Mount)

> **Geändert durch [ADR-0029](../decisions/0029-docker-compose-single-source-format.md):** Volumes
> werden nun in **Docker Compose** deklariert (Compose-Syntax, Bind-Mounts wo sinnvoll); die strikte
> „nur Named Volumes"-Vorgabe entfällt. Backup ist opt-in über die Sidecar-`d4y.yaml`.

Ein **Volume** (allgemeiner: ein **Mount**) ist der Ort, an dem eine
[Application](application.md) zur Laufzeit Daten ablegt. Volumes sind der Mechanismus, über den
**persistente** von **ephemeren** Daten unterschieden werden.

## Begriff

Eine App **deklariert** ihre Mounts im [Config-Repository](config-repository.md) — welcher
Pfad im Container mit Daten hinterlegt wird. Diese **Deklaration** ist Teil des Soll-Zustands.
Der **Inhalt** eines Volumes ist es hingegen **nicht** — Daten sind nicht aus Git
rekonstruierbar. → [desired-vs-actual-state](desired-vs-actual-state.md)

Es werden zwei Arten von Mounts unterschieden:

- **Named Volume** *(Standard)* — von der Container-Engine verwaltet, ohne festen Host-Pfad.
  Klar definierbar und damit sicherbar; die bevorzugte Variante für persistente Daten.
- **Bind Mount** — bindet einen konkreten Host-Pfad in den Container. Ausschließlich für
  **Host-Integration** vorgesehen (z. B. Sockets, injizierte Konfiguration), **nicht** als
  Standard für persistente Anwendungsdaten. Bind Mounts koppeln Daten an einen konkreten Host
  und erschweren einheitliches Backup.

## Persistent vs. ephemer

Ob die Daten eines Volumes einen Redeploy oder Serververlust überstehen, hängt allein davon ab,
ob für die App ein [Backup](backup.md) konfiguriert ist:

- **mit Backup** → Daten sind durable und werden auf einem frischen/leeren Volume
  wiederhergestellt.
- **ohne Backup** → Daten sind **ephemer** und gehen bei Redeploy/Serververlust verloren.

## Beziehungen

- Wird von einer [Application](application.md) deklariert.
- Seine Durability ergibt sich aus dem [Backup](backup.md).
- Nur die Deklaration, nicht der Inhalt, ist Teil des [Soll-Zustands](desired-vs-actual-state.md).

## Beispiel (Interim-YAML)

Im lokalen Desired-State (`desired/*.yaml`, [ADR-0011](../decisions/0011-interim-local-desired-state-source.md))
deklariert eine App ihre **Named Volumes** als Liste aus `name` (engine-verwaltetes Volume) und
`path` (Mount-Pfad im Container). Bind Mounts sind hier bewusst **nicht** vorgesehen.

```yaml
name: nginx
image: nginx:1.27-alpine
volumes:
  - name: html
    path: /usr/share/nginx/html
  - name: cache
    path: /var/cache/nginx
```

## Regeln

- Ein Volume wird **deklarativ** im Config-Repository beschrieben; nur die Deklaration ist Teil
  des Soll-Zustands, **nicht** der Inhalt.
- **Named Volumes** sind der Standard für persistente Daten.
- **Bind Mounts** sind ausschließlich für Host-Integration zulässig, nicht für persistente
  Anwendungsdaten.
- Ohne konfiguriertes [Backup](backup.md) sind die Daten eines Volumes **ephemer**; ihr Verlust
  bei Redeploy/Serververlust ist ein akzeptierter Zustand, kein Fehler.
