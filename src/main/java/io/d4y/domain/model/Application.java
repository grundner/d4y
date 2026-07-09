package io.d4y.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Eine deklarativ beschriebene Anwendung, die als Container laufen soll.
 *
 * <p>Aktuell: Name + Image + optionale <b>Named Volumes</b> ({@link VolumeMapping}), <b>Routes</b>
 * ({@link Route}) und deklarierte <b>Umgebungsvariablen</b> ({@code env}). Backup-Policy folgt in
 * einer späteren Ausbaustufe.
 */
public record Application(String name, ImageRef image, List<VolumeMapping> volumes,
                          List<Route> routes, Map<String, String> env) {

    public Application {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(image, "image");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Application-Name darf nicht leer sein");
        }
        volumes = volumes == null ? List.of() : List.copyOf(volumes);
        if (volumes.stream().map(VolumeMapping::name).distinct().count() != volumes.size()) {
            throw new IllegalArgumentException("Doppelte Volume-Namen in App '" + name + "'");
        }
        if (volumes.stream().map(VolumeMapping::path).distinct().count() != volumes.size()) {
            throw new IllegalArgumentException("Doppelte Volume-Pfade in App '" + name + "'");
        }
        routes = routes == null ? List.of() : List.copyOf(routes);
        if (routes.stream().map(r -> r.host() + r.path()).distinct().count() != routes.size()) {
            throw new IllegalArgumentException("Doppelte Route (Host+Pfad) in App '" + name + "'");
        }
        env = env == null ? Map.of() : Map.copyOf(env);
    }

    /** Bequemer Konstruktor mit Volumes und Routes, ohne deklariertes env. */
    public Application(String name, ImageRef image, List<VolumeMapping> volumes, List<Route> routes) {
        this(name, image, volumes, routes, Map.of());
    }

    /** Bequemer Konstruktor mit Volumes, ohne Routes und env. */
    public Application(String name, ImageRef image, List<VolumeMapping> volumes) {
        this(name, image, volumes, List.of(), Map.of());
    }

    /** Bequemer Konstruktor für Apps ohne Volumes, Routes und env. */
    public Application(String name, ImageRef image) {
        this(name, image, List.of(), List.of(), Map.of());
    }
}
