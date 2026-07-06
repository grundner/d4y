package io.d4y.domain.reconcile;

import java.util.List;
import java.util.Objects;

/**
 * Das Ergebnis eines Diff-Laufs: die Menge der auszuführenden Aktionen.
 */
public record ReconcilePlan(List<ReconcileAction> actions) {

    public ReconcilePlan {
        Objects.requireNonNull(actions, "actions");
        actions = List.copyOf(actions);
    }

    /** {@code true}, wenn ausschließlich {@link ReconcileAction.Noop}-Aktionen enthalten sind. */
    public boolean isInSync() {
        return actions.stream().allMatch(a -> a instanceof ReconcileAction.Noop);
    }

    /** Aktionen, die den Ist-Zustand tatsächlich verändern (ohne Noop). */
    public List<ReconcileAction> changingActions() {
        return actions.stream().filter(a -> !(a instanceof ReconcileAction.Noop)).toList();
    }
}
