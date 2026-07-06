# ADR-0004: Frontend Next.js/React — Sollzustand read-only, operative Aktionen erlaubt

Status: Proposed
Datum: 2026-07-06
Betrifft: [architecture/overview](../architecture/overview.md), [ui/status-view](../ui/status-view.md), [operational-action](../domain/operational-action.md), [ADR-0012](0012-operational-actions-and-reconciliation-hold.md)

## Kontext

Nutzer benötigen Einblick in den Plattformzustand und im Betrieb auch **operative Eingriffe**
(manueller Restart, Debugging via Logs/exec, temporäre Parameter). Frühere Fassung dieses ADR
verlangte ein **strikt** read-only Frontend. Das ist zu eng: Es verhindert legitime
Betriebsaktionen, die den **Sollzustand gar nicht ändern**.

Die tragende Invariante ist nicht „die UI darf nichts schreiben", sondern:
**Git ist die einzige Quelle der Wahrheit für den _Sollzustand_**
([ADR-0001](0001-git-as-single-source-of-truth.md)).

## Entscheidung

Das Frontend wird mit **Next.js** und **React** umgesetzt. Es ist **read-only bezüglich des
Sollzustands**: Es kann den deklarativen Sollzustand (welche Apps, Images, Routes, Volumes)
**niemals** ändern — das bleibt ausschließlich Git.

Darüber hinaus darf es **operative Aktionen** auslösen, die den Sollzustand nicht verändern
(Inspizieren/Debuggen, Lifecycle-Nudges, temporäre Parameter). Solche Aktionen sind bewusst
**transient**, werden auditiert und — wo nötig — über einen zeitlich begrenzten
[Reconciliation-Hold](../domain/reconciliation-hold.md) ermöglicht. Details:
[ADR-0012](0012-operational-actions-and-reconciliation-hold.md).

## Konsequenzen

- **Positiv:** Realer Betrieb (Restart, Debugging) ist möglich, ohne die Git-Wahrheit über den
  Sollzustand aufzuweichen.
- **Positiv:** Klare Grenze — Sollzustand-Änderungen nur via Git, alles andere ist transiente,
  sichtbare, auditierte Abweichung.
- **Negativ:** Die UI/API erhält schreibende Endpunkte; deren Grenze (kein Sollzustand-Schreiben)
  muss technisch und organisatorisch durchgesetzt werden.

## Alternativen

- **Strikt read-only** (frühere Fassung) — verworfen: verhindert legitime Betriebsaktionen.
- **UI schreibt den Sollzustand nach Git zurück** — verworfen (für diese Ausbaustufe): die UI
  würde de facto zum Editor der Wahrheit; widerspricht der klaren Trennung.
