# Architektur — Networking & DNS

Status: Draft
Bezug: [ADR-0010](../decisions/0010-dns-ingress-service-discovery.md),
[route](../domain/route.md), [dns-provider](../domain/dns-provider.md),
[service-discovery](../domain/service-discovery.md), [reconciliation-loop](reconciliation-loop.md)

„DNS" umfasst in D4Y drei getrennte Concerns, die unterschiedlich ins Git-native-Modell passen:

| Ebene | Concern | Domänenobjekt |
| --- | --- | --- |
| Intern (east-west) | App findet App | [service-discovery](../domain/service-discovery.md) |
| Extern / Ingress (north-south) | Hostname → App | [route](../domain/route.md) |
| Autoritative DNS-Records | `host → IP` | [dns-provider](../domain/dns-provider.md) *(managed-Modus)* |

## Interne Service Discovery

Jede App erhält einen **stabilen logischen Namen** aus ihrer Deklaration. Die
Plattform-Netzwerkschicht löst App-zu-App über diesen Namen auf (engine-internes DNS auf einem
definierten Netzwerk). Apps kennen keine Server-IPs.

## Externer Ingress via Routes

[Routes](../domain/route.md) (Hostname → App) sind ein First-Class-Objekt im Config-Repo. Aus
ihnen leitet die Plattform die Konfiguration eines **Reverse Proxy** ab, der eingehenden Verkehr
an den passenden Container weiterreicht:

```text
   Internet
      │  Hostname
      ▼
  ┌───────────────┐   liest Routes aus dem
  │ Reverse Proxy │ ◀─ Config-Repo (Soll)
  └──────┬────────┘
         │ Hostname → App
         ▼
   Container der Ziel-App
```

## Autoritative DNS-Records — zwei Modi

Austauschbare Server haben keine stabile IP. Ein öffentlicher A-Record auf eine konkrete
Server-IP würde die Wegwerfbarkeit brechen. D4Y bietet daher zwei **konfigurierbare** Modi:

```text
  (A) MANAGED                              (B) EXTERN / stabiler Eintrittspunkt
  ┌──────────────┐                         ┌──────────────────────────┐
  │ DNS-Provider │ ◀─ D4Y aktualisiert     │ Floating IP / externer LB│ ◀─ Betreiber stellt
  │ (Records)    │    Records aus dem Soll │ (stabile Adresse)        │    stabilen Eintritt bereit
  └──────┬───────┘    auch bei Serverwechsel└───────────┬─────────────┘
         ▼                                              ▼
   zeigt auf aktuellen Eintrittspunkt            DNS (extern) zeigt hierauf;
                                                  D4Y fasst öffentliches DNS NICHT an
```

- **(A) managed:** DNS-Records werden im Config-Repo deklariert; D4Y programmiert einen
  [DNS-Provider](../domain/dns-provider.md) und hält Records aktuell — auch wenn sich Server
  ändern.
- **(B) extern:** DNS zeigt auf einen stabilen Eintrittspunkt, den der Betreiber bereitstellt;
  D4Y verwaltet keine öffentlichen Records.

In **beiden** Modi gilt: die öffentliche Erreichbarkeit hängt nie an der IP eines konkreten
[Servers](../domain/server.md).

## Einordnung in den Reconciliation-Loop

Der [Reconciliation-Loop](reconciliation-loop.md) stellt beim Herstellen des Sollzustands sicher,
dass die interne Namensauflösung besteht, die Reverse-Proxy-Konfiguration den deklarierten
[Routes](../domain/route.md) entspricht und — im **managed-Modus** — die DNS-Records beim
[DNS-Provider](../domain/dns-provider.md) mit dem Soll übereinstimmen.

> Konkrete Wahl von Reverse Proxy, Overlay-/Netzwerktechnik, DNS-Protokollen und Provider-APIs
> sind **Implementierungsentscheidungen** und werden bei der Umsetzung per ADR festgelegt.

## Umsetzungsstand

- **Externer Ingress via Traefik** ist umgesetzt ([ADR-0016](../decisions/0016-reverse-proxy-traefik-docker-labels.md)):
  Apps deklarieren Routes im [Desired-State-YAML](desired-state-yaml.md) (`routes: [{host, path, port}]`).
  D4Y stellt einen verwalteten **Traefik**-Edge-Container (Docker-Provider, HTTP `:80`) sicher, hängt
  alle verwalteten Container an ein gemeinsames `d4y`-Netz (Alias = App-Name) und rendert je Route
  Traefik-Router/Service-Labels auf den App-Container. Routes sind Teil des Reconcile (Änderung ⇒ Replace).
- **Noch offen:** **TLS/ACME**, die beiden **DNS-Modi** (managed/extern) und die explizite interne
  **Service-Discovery** (das `d4y`-Netz legt die Grundlage) — jeweils eigene Ausbaustufe/ADR.
