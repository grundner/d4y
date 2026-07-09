package io.d4y.domain.model;

import java.util.LinkedHashMap;
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
        return new ContainerSpec(application.name(), application.image(), application.env(),
                application.volumes(), application.routes());
    }

    /**
     * Wie {@link #forApplication(Application)}, aber mit transientem Umgebungs-Override (operative
     * Aktion): das deklarierte {@code env} wird um {@code override} ergänzt/überschrieben.
     */
    public static ContainerSpec forApplication(Application application, Map<String, String> override) {
        Map<String, String> merged = new LinkedHashMap<>(application.env());
        if (override != null) {
            merged.putAll(override);
        }
        return new ContainerSpec(application.name(), application.image(), merged,
                application.volumes(), application.routes());
    }
}
