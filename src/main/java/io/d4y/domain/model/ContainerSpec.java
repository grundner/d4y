package io.d4y.domain.model;

import java.util.Map;
import java.util.Objects;

/**
 * Engine-neutrale Beschreibung eines zu startenden Containers.
 *
 * <p>Die Label-Konvention ({@link D4yLabels}) setzt der jeweilige Backend-Adapter.
 * {@code env} trägt optionale (temporäre) Umgebungs-Overrides für operative Aktionen.
 */
public record ContainerSpec(String appName, ImageRef image, Map<String, String> env) {

    public ContainerSpec {
        Objects.requireNonNull(appName, "appName");
        Objects.requireNonNull(image, "image");
        env = env == null ? Map.of() : Map.copyOf(env);
    }

    public static ContainerSpec forApplication(Application application) {
        return new ContainerSpec(application.name(), application.image(), Map.of());
    }

    public static ContainerSpec forApplication(Application application, Map<String, String> env) {
        return new ContainerSpec(application.name(), application.image(), env);
    }
}
