package io.d4y.port;

import io.d4y.domain.model.ContainerDetails;
import io.d4y.domain.model.ExecResult;

import java.util.List;

/**
 * Engine-neutraler Port für <b>container-nahe operative Aktionen</b> (ADR-0005/0013): Lifecycle-Nudges
 * und Inspektion auf einer Container-ID. Das Deployment selbst läuft über Docker Compose (ADR-0029),
 * nicht über diesen Port.
 */
public interface ContainerBackend {

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
