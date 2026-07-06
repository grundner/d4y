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
