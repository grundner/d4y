# Architektur — Bootstrap / Inbetriebnahme

Status: Draft
Bezug: [ADR-0008](../decisions/0008-bootstrap-single-command-install.md),
[config-repository](../domain/config-repository.md),
[server](../domain/server.md)

Eine frisch installierte Linux-Maschine soll ihre vollständige Laufzeitumgebung allein durch
**einen einzigen Bootstrap-Befehl** und ein Konfigurations-Repository wiederherstellen können.

> **Konkretisierung — 1-Zeiler & Voll-Push ([ADR-0027](../decisions/0027-d4y-host-bundle-systemd.md),
> [ADR-0025](../decisions/0025-full-push-desired-state-delivery.md)):**
> In der umgesetzten Variante liegt **alles auf GitHub** und d4y arbeitet **voll push-getrieben**. Der
> Bootstrap ist ein `curl … | sh`-Installer (GitHub Pages), der d4y als **selbst-enthaltendes Bundle**
> (App + eingebettetes JRE, **kein System-Java**) vom GitHub-Release lädt, direkt auf dem Host entpackt
> und als **systemd-Service** startet (kein Container für d4y selbst). Docker bleibt Pflicht — d4y
> orchestriert Traefik und Apps weiter über den Docker-Socket; die eigene Traefik-Route entsteht per
> File-Provider im Code. Abgefragt werden **Host + ACME-Mail** statt Config-Repo-Credentials — d4y hält
> **keine** GitHub-Credentials; Sollzustand und Secrets kommen per authentifiziertem Push.
> Der folgende konzeptionelle Ablauf (Pull-Modell) bleibt als optionale Alternative gültig.

## Was der Bootstrap abfragt

Während der Inbetriebnahme werden **nur** die minimal notwendigen Angaben erfragt:

1. **Git-Config-Repository** — URL/Referenz des Sollzustands.
2. **Zugangsdaten** — Credentials für Repo und ggf. Registries.

Es wird **keine** individuelle Server-Konfiguration abgefragt — Server sind austauschbar und
zustandslos. → [server](../domain/server.md)

## Ablauf (konzeptionell)

```text
  bootstrap-command
        │
        ▼
  Abfrage: Config-Repo + Credentials
        │
        ▼
  Container-Engine sicherstellen (Docker)
        │
        ▼
  D4Y-Runtime-Image beziehen & starten (Single-Image)
        │
        ▼
  Config-Repo klonen (Sollzustand laden)
        │
        ▼
  Reconciliation-Loop starten ──► Infrastruktur herstellen, Anwendungen starten
```

Ab diesem Punkt übernimmt der [Reconciliation-Loop](reconciliation-loop.md) die kontinuierliche
Herstellung des Sollzustands.

## Prinzipien

- **Single-Command** — kein mehrstufiges manuelles Setup.
- **Minimaler Input** — nur Config-Repo + Credentials.
- **Selbst-Einrichtung** — D4Y richtet Infrastruktur und Anwendungen eigenständig ein.
- **Reproduzierbar** — dieselbe Config führt auf jeder Maschine zum selben Zustand.

> Das konkrete Transportmittel des Bootstrap (Skript, Installer, Cloud-Init o. ä.) ist eine
> **Implementierungsentscheidung** und wird per ADR bei der Umsetzung festgelegt.
