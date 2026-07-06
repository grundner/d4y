package io.d4y.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Der Sollzustand: die Menge der Anwendungen, die laufen sollen.
 */
public record DesiredState(List<Application> applications) {

    public DesiredState {
        Objects.requireNonNull(applications, "applications");
        applications = List.copyOf(applications);
    }

    public static DesiredState empty() {
        return new DesiredState(List.of());
    }
}
