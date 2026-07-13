# ADR-0016: Reverse-Proxy für Ingress via Traefik (Docker-Label-Provider)

Status: Accepted
Datum: 2026-07-09
Betrifft: [route](../domain/route.md), [service-discovery](../domain/service-discovery.md), [architecture/networking-and-dns](../architecture/networking-and-dns.md), [ADR-0010](0010-dns-ingress-service-discovery.md), [ADR-0005](0005-container-backend-abstraction-docker-first.md)

> **Ergänzt durch [ADR-0027](0027-d4y-host-bundle-systemd.md):** Der Docker-Label-Provider bleibt für
> alle **verwalteten App-Container** maßgeblich. Da d4y selbst nicht mehr als Container läuft, wird
> **ausschließlich d4ys eigene Route** über einen Traefik-**File-Provider** deklariert (Ziel
> `host.docker.internal:8080`).

## Kontext

[Routes](../domain/route.md) sind deklariert und sichtbar ([ADR-0010](0010-dns-ingress-service-discovery.md),
erster Schnitt), aber **nicht funktional** — es gibt keinen Reverse Proxy, der eingehenden Verkehr
an die Ziel-Container weiterreicht. [networking-and-dns](../architecture/networking-and-dns.md)
hat die **konkrete Proxy-Wahl** ausdrücklich einer eigenen ADR überlassen.

D4Y verwaltet Container Docker-nativ und stempelt bereits Labels ([ADR-0005](0005-container-backend-abstraction-docker-first.md),
Docker-Adapter). Das legt einen label-basierten Proxy nahe, der Router-Konfiguration **direkt aus
Container-Labels** liest — ohne separate Config-Datei und Reload.

## Entscheidung

1. **Traefik v3 als von D4Y verwalteter Edge-Container** (`d4y_traefik`): Docker-Provider aktiv,
   HTTP-Entrypoint `web` auf `:80` (host-published), Docker-Socket **read-only** gemountet, läuft
   auf dem gemeinsamen `d4y`-Netz. Wird — wie ein System-Baustein — idempotent sichergestellt.
2. **Gemeinsames Docker-Netzwerk `d4y`** (bridge, `d4y.managed`): Traefik **und alle** verwalteten
   App-Container hängen daran; App-Container erhalten den **Netzwerk-Alias = App-Name** (Grundlage
   für die interne [Service-Discovery](../domain/service-discovery.md), ADR-0010 Punkt 3).
3. **Routes → Traefik-Router-Labels** auf dem App-Container. D4Y rendert je Route einen Router
   (`Host(...)`, optional `PathPrefix(...)`) und einen Service (Ziel-Port). Die Übersetzung
   Route→Label lebt **im Docker-Adapter** (engine-spezifisch); die Kernlogik trägt Routes nur als
   engine-neutrale Daten. Kein Config-File, kein Reload.
4. **Optionaler `port` je Route** (Default `80`) bestimmt den Ziel-Port des Traefik-Service.
5. **Drift:** Router-Labels und Netzanbindung werden beim Erstellen des Containers gebacken; eine
   geänderte Route-Deklaration ist daher **Drift** und führt zu einem Container-**Replace**
   (analog Image/Volumes).
6. **Erster Schnitt: HTTP-only.** TLS/ACME, die DNS-Modi (managed/extern, ADR-0010) und weitere
   Proxy-Backends sind spätere Ausbaustufen.

## Konsequenzen

- **Positiv:** Nutzt die vorhandene Label-/Docker-Architektur — minimaler Zusatzcode, keine
  Config-Generierung/Reload. Das `d4y`-Netz schafft zugleich die Grundlage für interne
  Service-Discovery.
- **Positiv:** Routes werden Teil des deklarativen Reconcile — Abweichungen heilen selbst.
- **Negativ:** Traefik-Kopplung im Docker-Adapter (engine-spezifisch — vertretbar, da im Adapter,
  nicht im Kern).
- **Negativ:** Traefik benötigt **Docker-Socket-Zugriff** (Angriffsfläche; daher read-only und als
  bewusst dokumentierte Ausnahme).
- **Negativ:** Bestehende Container werden durch den Wechsel auf das `d4y`-Netz **einmalig** ersetzt.

## Alternativen

- **nginx/Caddy + generierte Config-Datei** — verworfen für den ersten Schnitt: mehr bewegliche
  Teile (Datei-Management, Reload/Admin-API), ohne die Label-Architektur zu nutzen.
- **Reverse Proxy als gewöhnliche App ohne First-Class-Routing** — bereits in
  [ADR-0010](0010-dns-ingress-service-discovery.md) verworfen (Routing wäre intransparent).
- **Traefik File-Provider statt Docker-Provider** — verworfen: erfordert Config-Generierung/Reload,
  obohl die Label-Quelle direkt am Container verfügbar ist.
