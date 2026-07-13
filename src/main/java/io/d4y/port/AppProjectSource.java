package io.d4y.port;

import io.d4y.domain.model.AppProject;

import java.util.List;

/**
 * Quelle der Compose-App-Projekte des Sollzustands (ADR-0029): enumeriert die App-Verzeichnisse
 * (Git-Klon oder lokaler Desired-Pfad). Engine-neutral.
 */
public interface AppProjectSource {

    /** Alle App-Projekte im aktuellen Sollzustand (leer, wenn kein Verzeichnis existiert). */
    List<AppProject> load();
}
