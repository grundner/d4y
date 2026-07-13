package io.d4y.domain.model;

import java.nio.file.Path;

/**
 * Eine App als <b>Docker-Compose-Projekt</b> (ADR-0029): ein Verzeichnis mit einer {@code compose.yaml}
 * und optionaler Sidecar {@code d4y.yaml}. Der App-Name ist der Verzeichnisname; das Compose-Projekt
 * heißt {@code <prefix><name>}.
 *
 * @param name        App-/Verzeichnisname
 * @param directory   Verzeichnis der App (Basis für relative Build-Kontexte/{@code .env})
 * @param composeFile Pfad zur {@code compose.yaml}/{@code docker-compose.yml}
 * @param sidecar     Pfad zur {@code d4y.yaml} oder {@code null}, falls nicht vorhanden
 */
public record AppProject(String name, Path directory, Path composeFile, Path sidecar) {

    public boolean hasSidecar() {
        return sidecar != null;
    }

    /** Compose-Projektname für {@code docker compose -p ...}. */
    public String projectName(String prefix) {
        return prefix + name;
    }
}
