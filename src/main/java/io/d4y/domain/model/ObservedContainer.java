package io.d4y.domain.model;

import java.util.Objects;

/**
 * Ein tatsächlich beobachteter, von D4Y verwalteter Container (Ist-Zustand).
 *
 * @param id       Container-ID der Engine
 * @param appName  Name der zugehörigen Application (aus dem {@code d4y.app}-Label)
 * @param image    Image-Referenz, mit der der Container erzeugt wurde ({@code d4y.image}-Label)
 * @param running  {@code true}, wenn der Container läuft
 */
public record ObservedContainer(String id, String appName, ImageRef image, boolean running) {

    public ObservedContainer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(appName, "appName");
        Objects.requireNonNull(image, "image");
    }
}
