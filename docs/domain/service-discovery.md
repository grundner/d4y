# Domäne — Service-Discovery

**Service-Discovery** beschreibt, wie [Applications](application.md) einander **intern**
(east-west) finden und ansprechen — im Gegensatz zur externen Erreichbarkeit über eine
[Route](route.md).

## Begriff

Jede App ist über einen **stabilen logischen Namen** erreichbar, der aus ihrer Deklaration im
[Config-Repository](config-repository.md) abgeleitet wird. Die Plattform-Netzwerkschicht löst
App-zu-App-Kommunikation über diesen Namen auf; eine App muss die konkrete Adresse oder den
[Server](server.md) einer anderen App nicht kennen.

Der logische Name ist **stabil** über Neustarts, Redeploys und Serverwechsel hinweg — er hängt
nur von der Deklaration ab, nicht vom aktuellen Ist-Zustand.

## Beziehungen

- Jede [Application](application.md) besitzt einen stabilen logischen Namen.
- Ergänzt die [Route](route.md): Routes regeln externen Ingress, Service-Discovery die interne
  Adressierung.
- Der logische Name ist Teil des [Desired-vs-Actual-State](desired-vs-actual-state.md) (Soll).

## Regeln

- Jede App ist intern über einen **stabilen, aus ihrer Deklaration abgeleiteten** Namen erreichbar.
- Der logische Name bleibt über Neustarts, Redeploys und Serverwechsel **stabil**.
- Interne Adressierung erfolgt **ausschließlich** über den logischen Namen, nicht über konkrete
  Server-IPs.
- Service-Discovery betrifft nur **interne** Kommunikation; externer Zugriff läuft über eine
  [Route](route.md).
