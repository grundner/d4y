# Domäne — Container-Image

> **Geändert durch [ADR-0029](../decisions/0029-docker-compose-single-source-format.md):** Images
> werden aus Registries **bezogen** oder auf dem Ziel **gebaut** (`build:` in Compose). Die frühere
> Regel „niemals auf dem Zielsystem gebaut" gilt nicht mehr.

Ein **Container-Image** ist ein unveränderliches Artefakt, aus dem eine
[Application](application.md) bereitgestellt wird.

## Begriff

Ein Container-Image ist **immutable**: Sein Inhalt ändert sich nach der Erstellung nicht mehr.
Es wird eindeutig über seine Referenz identifiziert und stammt aus einer vertrauenswürdigen
[Registry](registry.md). Dieselbe Image-Referenz führt in jeder Umgebung zum selben Inhalt und
ermöglicht so deterministische, reproduzierbare Deployments.

## Beziehungen

- Wird von einer [Application](application.md) referenziert.
- Wird aus einer [Registry](registry.md) bezogen.

## Regeln

- Ein Container-Image ist **unveränderlich** — es wird nach der Erstellung nicht modifiziert.
- Ein Image wird ausschließlich aus einer **vertrauenswürdigen** [Registry](registry.md) bezogen.
- Dieselbe Image-Referenz **muss** über alle Umgebungen hinweg denselben Inhalt liefern
  (deterministische Deployments).
- Images werden **niemals** auf dem Zielsystem gebaut, sondern nur bezogen.
