package io.d4y.domain.reconcile;

import io.d4y.domain.model.Application;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.ObservedContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kern der Reconciliation: leitet aus Soll- und Ist-Zustand einen {@link ReconcilePlan} ab.
 *
 * <p>Bewusst <b>seiteneffektfrei und framework-frei</b> — dadurch vollständig testbar, ohne
 * eine reale Container-Engine. Das Ausführen des Plans übernimmt eine andere Komponente.
 */
public class Reconciler {

    /**
     * Vergleicht den Sollzustand mit den beobachteten Containern und liefert die nötigen Aktionen.
     *
     * <ul>
     *   <li>App deklariert, kein Container → {@link ReconcileAction.Start}</li>
     *   <li>Container vorhanden, aber falsches Image oder gestoppt → {@link ReconcileAction.Replace}</li>
     *   <li>Container passt und läuft → {@link ReconcileAction.Noop} (idempotent)</li>
     *   <li>Verwalteter Container ohne Deklaration → {@link ReconcileAction.StopAndRemove} (Drift)</li>
     * </ul>
     */
    public ReconcilePlan plan(DesiredState desired, List<ObservedContainer> actual) {
        return plan(desired, actual, Set.of());
    }

    /**
     * Wie {@link #plan(DesiredState, List)}, überspringt aber Ziele mit aktivem
     * Reconciliation-Hold: für sie wird nichts verändert (Noop). → ADR-0012
     */
    public ReconcilePlan plan(DesiredState desired, List<ObservedContainer> actual, Set<String> heldAppNames) {
        Map<String, ObservedContainer> byApp = actual.stream()
                .collect(Collectors.toMap(ObservedContainer::appName, Function.identity(), (a, b) -> a));

        List<ReconcileAction> actions = new ArrayList<>();
        Set<String> desiredNames = new HashSet<>();

        for (Application app : desired.applications()) {
            desiredNames.add(app.name());
            if (heldAppNames.contains(app.name())) {
                actions.add(new ReconcileAction.Noop(app.name())); // gehalten
                continue;
            }
            ObservedContainer observed = byApp.get(app.name());
            if (observed == null) {
                actions.add(new ReconcileAction.Start(app));
            } else if (!matches(app, observed)) {
                actions.add(new ReconcileAction.Replace(app, observed.id()));
            } else {
                actions.add(new ReconcileAction.Noop(app.name()));
            }
        }

        // Verwaltete Container, die nicht (mehr) deklariert sind: Drift bereinigen —
        // es sei denn, das Ziel wird gehalten.
        for (ObservedContainer observed : actual) {
            if (!desiredNames.contains(observed.appName()) && !heldAppNames.contains(observed.appName())) {
                actions.add(new ReconcileAction.StopAndRemove(observed.appName(), observed.id()));
            }
        }

        return new ReconcilePlan(actions);
    }

    /** Ein Container passt, wenn er läuft und mit der deklarierten Image-Referenz erzeugt wurde. */
    private boolean matches(Application app, ObservedContainer observed) {
        return observed.running() && observed.image().equals(app.image());
    }
}
