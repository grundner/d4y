# Architektur — Operative Aktionen & Hold

Status: Draft
Bezug: [ADR-0012](../decisions/0012-operational-actions-and-reconciliation-hold.md),
[ADR-0004](../decisions/0004-nextjs-react-readonly-frontend.md),
[operational-action](../domain/operational-action.md),
[reconciliation-hold](../domain/reconciliation-hold.md),
[reconciliation-loop](reconciliation-loop.md)

Die Plattform ist **read-only bezüglich des Sollzustands** (nur Git ändert ihn), erlaubt aber
**operative Aktionen** über UI/API, die den Sollzustand nicht verändern. Dieses Dokument
beschreibt, wie das mit dem kontinuierlichen Reconciler zusammenspielt.

## Zwei Klassen von Änderungen

| Klasse | Beispiel | Quelle | Autoritativ? | Persistenz |
| --- | --- | --- | --- | --- |
| Sollzustand-Änderung | App/Image/Route/Volume | **nur Git** | ja | versioniert |
| Operative Aktion | Restart, Logs/exec, temp. Parameter | UI/API | **nein** | transient |

## Der Konflikt und seine Lösung

Der [Reconciliation-Loop](reconciliation-loop.md) korrigiert jede Abweichung vom Sollzustand.
Ein manueller Stop oder ein temporärer Parameter wäre damit sofort wieder weg. Lösung: ein
zeitlich begrenzter [Reconciliation-Hold](../domain/reconciliation-hold.md).

```text
  Operator löst Aktion aus (z. B. temporärer Parameter)
        │
        ▼
  Hold für Ziel setzen (zeitlich begrenzt)  ──► Reconciler überspringt dieses Ziel
        │
        ▼
  Aktion anwenden (imperativ, transient)   ──► als sanktionierte Drift sichtbar + auditiert
        │
        ▼
  Hold läuft ab (oder wird freigegeben)
        │
        ▼
  Reconciler greift wieder ──► Sollzustand wird wiederhergestellt (Self-Healing)
```

Reine Nudges (Restart) und Inspektion (Logs/exec) benötigen keinen Hold: Restart stellt ohnehin
den Sollzustand her, Inspektion verändert nichts.

## Einordnung in den Reconciliation-Loop

Der Loop prüft je Ziel, ob ein aktiver Hold vorliegt; solche Ziele werden **übersprungen**
(nicht global pausiert). Nach Ablauf des Holds wird das Ziel wieder normal abgeglichen.
→ [reconciliation-loop](reconciliation-loop.md)

## Sichtbarkeit & Audit

Operative Aktionen und aktive Holds erscheinen im [Status](../ui/status-view.md) als
**temporäre, sanktionierte Drift** und werden protokolliert. So bleibt jederzeit erkennbar, wo
die Plattform bewusst vom Sollzustand abweicht und wann sie dorthin zurückkehrt.

> Konkrete Endpunkte, Zugriffskontrolle, Audit-Ablage und Hold-Höchstdauern sind
> **Implementierungsentscheidungen** und werden bei der Umsetzung per ADR festgelegt.
