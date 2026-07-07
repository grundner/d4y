package io.d4y.adapter.docker;

/**
 * Die Docker-Engine ist nicht erreichbar — der Verbindungsaufbau zum Unix-Socket scheitert
 * (Socket-Datei fehlt oder der Daemon läuft nicht).
 *
 * <p>Abgegrenzt von {@link DockerApiException}: dort <em>antwortet</em> die Engine mit einem
 * Fehlerstatus; hier kommt gar keine Verbindung zustande. Der Reconciliation-Loop behandelt das
 * als vorübergehend (Retry), die API bildet es auf HTTP 503 ab (siehe {@code ApiExceptionHandler}).
 */
public class DockerUnavailableException extends RuntimeException {

    public DockerUnavailableException(String socketPath, Throwable cause) {
        super("Docker-Engine nicht erreichbar (Socket " + socketPath + "): " + rootMessage(cause), cause);
    }

    private static String rootMessage(Throwable cause) {
        String msg = cause == null ? null : cause.getMessage();
        // Netty liefert bei Socket-Connect-Fehlern oft eine null-Message — dann die Klasse nennen.
        return (msg == null || msg.isBlank()) && cause != null ? cause.getClass().getSimpleName() : msg;
    }
}
