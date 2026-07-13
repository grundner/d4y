package io.d4y.adapter.compose;

/** Fehler beim Ausführen eines {@code docker compose}-Kommandos (Exit != 0, Timeout, Start-Fehler). */
public class ComposeException extends RuntimeException {

    public ComposeException(String message) {
        super(message);
    }

    public ComposeException(String message, Throwable cause) {
        super(message, cause);
    }
}
