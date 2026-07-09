package io.d4y.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Eine deklarativ beschriebene Anwendung, die als Container laufen soll.
 *
 * <p>Aktuell: Name + Image + optionale <b>Named Volumes</b> ({@link VolumeMapping}). Backup-Policy
 * und Routes kommen in späteren Ausbaustufen hinzu.
 */
public record Application(String name, ImageRef image, List<VolumeMapping> volumes) {

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
    }

    /** Bequemer Konstruktor für Apps ohne Volumes. */
    public Application(String name, ImageRef image) {
        this(name, image, List.of());
    }
}
