package io.d4y.domain.model;

import java.util.Objects;

/**
 * Engine-neutrale Beschreibung eines zu startenden Containers.
 *
 * <p>Die Label-Konvention ({@link D4yLabels}) setzt der jeweilige Backend-Adapter.
 */
public record ContainerSpec(String appName, ImageRef image) {

    public ContainerSpec {
        Objects.requireNonNull(appName, "appName");
        Objects.requireNonNull(image, "image");
    }

    public static ContainerSpec forApplication(Application application) {
        return new ContainerSpec(application.name(), application.image());
    }
}
