package io.d4y.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Engine-neutrale Beschreibung eines zu startenden Containers.
 *
 * <p>{@code env} trägt deklarierte + ggf. transient überschriebene Umgebungsvariablen,
 * {@code volumes} die Named Volumes, {@code routes} den Ingress und {@code backup} das
 * Backup-Opt-in (steuert Restore-on-new).
 */
public record ContainerSpec(String appName, ImageRef image, Map<String, String> env,
                            List<VolumeMapping> volumes, List<Route> routes, boolean backup) {

    public ContainerSpec {
        Objects.requireNonNull(appName, "appName");
        Objects.requireNonNull(image, "image");
        env = env == null ? Map.of() : Map.copyOf(env);
        volumes = volumes == null ? List.of() : List.copyOf(volumes);
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    /** Bequemer Konstruktor ohne Routes/Backup. */
    public ContainerSpec(String appName, ImageRef image, Map<String, String> env, List<VolumeMapping> volumes) {
        this(appName, image, env, volumes, List.of(), false);
    }

    /** Bequemer Konstruktor ohne Backup. */
    public ContainerSpec(String appName, ImageRef image, Map<String, String> env,
                         List<VolumeMapping> volumes, List<Route> routes) {
        this(appName, image, env, volumes, routes, false);
    }

    public static ContainerSpec forApplication(Application application) {
        return new ContainerSpec(application.name(), application.image(), application.env(),
                application.volumes(), application.routes(), application.backup());
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
                application.volumes(), application.routes(), application.backup());
    }
}
