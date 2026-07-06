package io.d4y.port;

import io.d4y.domain.model.DesiredState;

/**
 * Quelle des Sollzustands.
 *
 * <p>ADR-0011 (Interim): erste Implementierung liest aus einem lokalen Verzeichnis; die
 * Git-Anbindung (ADR-0001) folgt und ersetzt dieses Interim.
 */
public interface DesiredStateSource {

    DesiredState load();
}
