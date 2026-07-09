package io.d4y.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Engine-neutrale Beschreibung eines zu startenden Containers.
 *
 * <p>Die Label-Konvention ({@link D4yLabels}) setzt der jeweilige Backend-Adapter.
 * {@code env} trägt optionale (temporäre) Umgebungs-Overrides für operative Aktionen;
 * {@code volumes} die deklarierten Named Volumes ({@link VolumeMapping}).
 */
public record ContainerSpec(String appName, ImageRef image, Map<String, String> env, List<VolumeMapping> volumes) {

    public ContainerSpec {
        Objects.requireNonNull(appName, "appName");
        Objects.requireNonNull(image, "image");
        env = env == null ? Map.of() : Map.copyOf(env);
        volumes = volumes == null ? List.of() : List.copyOf(volumes);
    }

    public static ContainerSpec forApplication(Application application) {
        return new ContainerSpec(application.name(), application.image(), Map.of(), application.volumes());
    }

    public static ContainerSpec forApplication(Application application, Map<String, String> env) {
        return new ContainerSpec(application.name(), application.image(), env, application.volumes());
    }
}
