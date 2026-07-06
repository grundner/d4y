# Architektur — Container-Backend-Abstraktion

Status: Draft
Bezug: [ADR-0005](../decisions/0005-container-backend-abstraction-docker-first.md)

D4Y steuert Container **nicht direkt** gegen eine konkrete Engine, sondern über eine
abstrakte Backend-Schnittstelle. Die erste Implementierung nutzt **Docker**; weitere
Backends (z. B. Podman, containerd) sollen ohne Änderung der Kernlogik ergänzbar sein.

## Ports & Adapter (konzeptionell)

```text
    Kernlogik (Reconciliation)
             │
             ▼  spricht nur gegen den Port
   ┌──────────────────────┐
   │ ContainerBackend     │  (Port — engine-neutral)
   └──────────┬───────────┘
              │ implementiert durch Adapter
   ┌──────────┴───────────┐
   │ DockerBackend        │  (Adapter — erste Impl.)
   └──────────────────────┘
```

Die Kernlogik kennt ausschließlich engine-neutrale Begriffe (Container, Image, Zustand). Alle
Engine-Spezifika (Docker-API, Sockets, Labels) leben im jeweiligen Adapter.

## Fähigkeiten des Ports (fachlich, nicht technisch festgelegt)

Der Port beschreibt die minimal nötigen Fähigkeiten, damit die Reconciliation arbeiten kann:

- **Beobachten** — laufende Container und deren Ist-Zustand ermitteln.
- **Image sicherstellen** — ein unveränderliches Image aus einer Registry verfügbar machen.
- **Starten / Stoppen / Ersetzen** — Container gemäß Sollzustand herstellen.
- **Zustand melden** — Health/Status für Reconciliation und Frontend liefern.

> Die konkrete Signatur/Technik dieser Fähigkeiten ist eine **Implementierungsentscheidung**
> und wird erst bei der Umsetzung (nicht in Domänendokumenten) festgelegt.

## Designziele

1. **Engine-Neutralität** — Kernlogik enthält keine Docker-Abhängigkeit.
2. **Ein Adapter pro Engine** — klare Isolation der Engine-Spezifika.
3. **Austauschbarkeit** — ein neues Backend erfordert nur einen neuen Adapter.

Siehe auch: [reconciliation-loop.md](reconciliation-loop.md),
[container-image](../domain/container-image.md).
