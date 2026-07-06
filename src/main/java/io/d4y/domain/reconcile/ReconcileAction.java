package io.d4y.domain.reconcile;

import io.d4y.domain.model.Application;

/**
 * Eine einzelne Aktion, die der Reconciler zur Herstellung des Sollzustands ableitet.
 */
public sealed interface ReconcileAction {

    String appName();

    /** Die App fehlt und wird neu gestartet. */
    record Start(Application application) implements ReconcileAction {
        @Override
        public String appName() {
            return application.name();
        }
    }

    /** Ein vorhandener Container weicht ab (falsches Image / gestoppt) und wird ersetzt. */
    record Replace(Application application, String currentContainerId) implements ReconcileAction {
        @Override
        public String appName() {
            return application.name();
        }
    }

    /** Ein verwalteter Container ist nicht (mehr) deklariert und wird entfernt (Drift). */
    record StopAndRemove(String appName, String containerId) implements ReconcileAction {
    }

    /** Soll und Ist stimmen überein; keine Änderung nötig. */
    record Noop(String appName) implements ReconcileAction {
    }
}
