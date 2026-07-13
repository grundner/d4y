# Domäne — Route (Ingress)

Eine **Route** beschreibt die externe Erreichbarkeit einer [Application](application.md): die
Zuordnung eines **Hostnamens** (und ggf. Pfads) zu einer laufenden App. Routes sind ein
**First-Class**-Domänenobjekt und werden deklarativ im [Config-Repository](config-repository.md)
beschrieben.

## Begriff

Eine Route bildet einen von außen erreichbaren Hostnamen auf eine App ab. Die Plattform kennt
alle Routes, kann sie visualisieren und leitet daraus die Konfiguration eines Reverse Proxy ab,
der eingehenden Verkehr an den passenden Container weiterreicht.

Die Route beschreibt **was** erreichbar sein soll (Hostname → App) — nicht, mit welchem konkreten
Proxy oder über welche IP das technisch geschieht.

Eine Route kann **HTTP oder HTTPS** sein ([ADR-0028](../decisions/0028-per-route-tls-and-http-mode.md)):
über das optionale `tls`-Feld (`true`/`false`). Ohne Angabe gilt der globale TLS-Default (aus der
ACME-Konfiguration abgeleitet). So sind reine HTTP-Routen möglich — etwa im Intranet/in einer VM ohne
öffentliche IP oder für lokale Tests.

## Beziehungen

- Verweist auf genau eine [Application](application.md) als Ziel.
- Ihr Hostname kann über einen [DNS-Provider](dns-provider.md) autoritativ aufgelöst werden
  (managed-Modus) oder über einen externen, stabilen Eintrittspunkt.
- Interne App-zu-App-Adressierung ist **nicht** Sache einer Route, sondern der
  [Service-Discovery](service-discovery.md).
- Ihr Soll ist Teil des [Desired-vs-Actual-State](desired-vs-actual-state.md).

## Regeln

- Eine Route wird **deklarativ** im Config-Repository beschrieben; Änderungen erfolgen
  ausschließlich dort.
- Eine Route ordnet einen Hostnamen genau einer [Application](application.md) zu.
- Die öffentliche Erreichbarkeit einer Route hängt **nicht** von der IP eines konkreten
  [Servers](server.md) ab.
- Routes betreffen ausschließlich **externen** Ingress; interne Adressierung erfolgt über
  [Service-Discovery](service-discovery.md).
- TLS ist **pro Route** wählbar ([ADR-0028](../decisions/0028-per-route-tls-and-http-mode.md)):
  `tls: true` = HTTPS, `tls: false` = reines HTTP, keine Angabe = globaler Default. Es gibt **keinen**
  automatischen HTTP→HTTPS-Redirect; eine Route bedient genau ein Schema.
