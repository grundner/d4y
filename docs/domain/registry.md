# Domäne — Registry

Eine **Registry** ist die Bezugsquelle, aus der [Container-Images](container-image.md)
geladen werden.

## Begriff

Eine Registry ist der vertrauenswürdige Ort, an dem unveränderliche Images bereitgestellt und
von der Plattform abgerufen werden. Nur Images aus als vertrauenswürdig eingestuften Registries
dürfen bereitgestellt werden.

## Beziehungen

- Stellt [Container-Images](container-image.md) bereit.
- Wird über das [Config-Repository](config-repository.md) referenziert/konfiguriert.

## Regeln

- Es werden ausschließlich Images aus **vertrauenswürdigen** Registries bezogen.
- Die Menge der vertrauenswürdigen Registries wird **deklarativ** im Config-Repository festgelegt.
- Der Zugriff auf eine Registry erfolgt über hinterlegte Zugangsdaten; deren Behandlung
  richtet sich nach den [Privacy-Rules](../rules/privacy-rules.md).
- **Registry-Zugangsdaten liegen weder im Config-Repository noch in d4ys Properties.** Im
  Sollzustand steht nur ein Platzhalter (`${secret:NAME}`); der Wert wird extern (z. B. GitHub)
  gehalten und per authentifiziertem Push geliefert
  ([ADR-0024](../decisions/0024-delivered-image-secrets-encrypted-store.md)). Beim Pull sendet d4y
  den `X-Registry-Auth`-Header aus dem aufgelösten Wert.
