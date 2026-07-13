package io.d4y.domain.model;

import java.util.List;

/**
 * Beobachteter Ist-Zustand eines Compose-Projekts (ADR-0029): der Projektname und die
 * dazugehörigen Container/Services.
 */
public record ComposeProject(String name, List<Container> containers) {

    /** Ein Container/Service eines Compose-Projekts. */
    public record Container(String id, String service, String image, String state, String status) {

        public boolean running() {
            return "running".equalsIgnoreCase(state);
        }
    }

    /** Aggregierter Zustand: RUNNING (alle laufen), PARTIAL (einige), STOPPED (keiner/leer). */
    public String state() {
        if (containers.isEmpty()) {
            return "STOPPED";
        }
        long running = containers.stream().filter(Container::running).count();
        if (running == containers.size()) {
            return "RUNNING";
        }
        return running == 0 ? "STOPPED" : "PARTIAL";
    }

    public boolean allRunning() {
        return !containers.isEmpty() && containers.stream().allMatch(Container::running);
    }

    /** Container-ID des ersten Service (für operative Aktionen), oder {@code null}. */
    public String primaryContainerId() {
        return containers.isEmpty() ? null : containers.get(0).id();
    }
}
