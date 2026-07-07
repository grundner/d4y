package io.d4y.port;

import io.d4y.domain.model.ContainerDetails;
import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.ExecResult;
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

    // --- Operative Aktionen (ADR-0013) ---------------------------------------------------

    /** Startet den Container neu. */
    void restart(String containerId);

    /** Stoppt den Container, ohne ihn zu entfernen. */
    void stop(String containerId);

    /** Liefert die letzten {@code tail} Log-Zeilen (stdout+stderr). */
    String logs(String containerId, int tail);

    /** Liefert Details des Containers (inspect). */
    ContainerDetails inspect(String containerId);

    /** Führt ein Kommando im Container aus und liefert Ausgabe + Exit-Code. */
    ExecResult exec(String containerId, List<String> cmd);
}
