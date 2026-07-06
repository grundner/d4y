package io.d4y.port;

import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.ImageRef;
import io.d4y.domain.model.ObservedContainer;

import java.util.List;

/**
 * Engine-neutraler Port zur Steuerung einer Container-Engine (ADR-0005).
 *
 * <p>Die Kernlogik spricht ausschließlich gegen diesen Port; Engine-Spezifika leben im Adapter
 * (erste Implementierung: Docker).
 */
public interface ContainerBackend {

    /** Liefert die aktuell von D4Y verwalteten Container (Ist-Zustand). */
    List<ObservedContainer> observe();

    /** Stellt sicher, dass das Image lokal verfügbar ist (Pull, falls nötig). */
    void ensureImage(ImageRef image);

    /** Erzeugt und startet einen Container gemäß Spezifikation; liefert dessen ID. */
    String run(ContainerSpec spec);

    /** Stoppt und entfernt den Container mit der angegebenen ID. */
    void stopAndRemove(String containerId);
}
