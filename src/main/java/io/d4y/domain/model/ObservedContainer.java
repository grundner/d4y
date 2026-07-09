package io.d4y.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Ein tatsächlich beobachteter, von D4Y verwalteter Container (Ist-Zustand).
 *
 * @param id       Container-ID der Engine
 * @param appName  Name der zugehörigen Application (aus dem {@code d4y.app}-Label)
 * @param image    Image-Referenz, mit der der Container erzeugt wurde ({@code d4y.image}-Label)
 * @param running  {@code true}, wenn der Container läuft
 * @param volumes  deklarierte Named Volumes, mit denen der Container erzeugt wurde
 *                 ({@code d4y.volumes}-Label; für den Drift-Vergleich)
 * @param routes   deklarierte Routes, mit denen der Container erzeugt wurde
 *                 ({@code d4y.routes}-Label; für den Drift-Vergleich)
 */
public record ObservedContainer(String id, String appName, ImageRef image, boolean running,
                                List<VolumeMapping> volumes, List<Route> routes) {

    public ObservedContainer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(appName, "appName");
        Objects.requireNonNull(image, "image");
        volumes = volumes == null ? List.of() : List.copyOf(volumes);
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    /** Bequemer Konstruktor ohne Volumes/Routes. */
    public ObservedContainer(String id, String appName, ImageRef image, boolean running) {
        this(id, appName, image, running, List.of(), List.of());
    }

    /** Bequemer Konstruktor ohne Routes. */
    public ObservedContainer(String id, String appName, ImageRef image, boolean running,
                             List<VolumeMapping> volumes) {
        this(id, appName, image, running, volumes, List.of());
    }
}
