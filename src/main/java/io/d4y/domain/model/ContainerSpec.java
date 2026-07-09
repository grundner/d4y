package io.d4y.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Engine-neutrale Beschreibung eines zu startenden Containers.
 *
 * <p>Die Label-Konvention ({@link D4yLabels}) setzt der jeweilige Backend-Adapter.
 * {@code env} trägt optionale (temporäre) Umgebungs-Overrides für operative Aktionen;
 * {@code volumes} die deklarierten Named Volumes ({@link VolumeMapping}), {@code routes} den
 * deklarierten externen Ingress ({@link Route}).
 */
public record ContainerSpec(String appName, ImageRef image, Map<String, String> env,
                            List<VolumeMapping> volumes, List<Route> routes) {

    public ContainerSpec {
        Objects.requireNonNull(appName, "appName");
        Objects.requireNonNull(image, "image");
        env = env == null ? Map.of() : Map.copyOf(env);
        volumes = volumes == null ? List.of() : List.copyOf(volumes);
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    /** Bequemer Konstruktor ohne Routes. */
    public ContainerSpec(String appName, ImageRef image, Map<String, String> env, List<VolumeMapping> volumes) {
        this(appName, image, env, volumes, List.of());
    }

    public static ContainerSpec forApplication(Application application) {
        return new ContainerSpec(application.name(), application.image(), Map.of(),
                application.volumes(), application.routes());
    }

    public static ContainerSpec forApplication(Application application, Map<String, String> env) {
        return new ContainerSpec(application.name(), application.image(), env,
                application.volumes(), application.routes());
    }
}
