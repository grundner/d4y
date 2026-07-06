package io.d4y.adapter.docker;

/**
 * Fehler bei der Kommunikation mit der Docker-Engine-API.
 */
public class DockerApiException extends RuntimeException {

    public DockerApiException(String action, int status, String body) {
        super("Docker-API-Fehler bei '" + action + "': HTTP " + status + " — " + body);
    }
}
