package io.d4y.domain.model;

import java.util.Objects;

/**
 * Eine deklarativ beschriebene Anwendung, die als Container laufen soll.
 *
 * <p>Minimalmodell des ersten Schnitts: Name + Image. Volumes, Backup-Policy und Routes
 * kommen in späteren Ausbaustufen hinzu.
 */
public record Application(String name, ImageRef image) {

    public Application {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(image, "image");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Application-Name darf nicht leer sein");
        }
    }
}
