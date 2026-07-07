package io.d4y.api;

import io.d4y.adapter.docker.DockerApiException;
import io.d4y.adapter.docker.DockerUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Übersetzt Backend-/Engine-Fehler in saubere HTTP-Antworten, statt rohe 500er samt
 * Reactive-Stacktrace ans Frontend zu geben. Unterstützt die „ehrliche Darstellung" von
 * Fehlerzuständen (docs/ui/status-view.md).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Maschinenlesbarer Fehler-Body für das Frontend. */
    public record ApiError(String error, String message) {
    }

    /** Docker-Engine nicht erreichbar → 503, damit das UI einen klaren „Fehler"-Zustand zeigt. */
    @ExceptionHandler(DockerUnavailableException.class)
    public ResponseEntity<ApiError> handleUnavailable(DockerUnavailableException e) {
        log.warn("Docker-Engine nicht erreichbar: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("docker-unavailable", e.getMessage()));
    }

    /** Engine erreichbar, aber mit Fehlerstatus geantwortet → 502 Bad Gateway. */
    @ExceptionHandler(DockerApiException.class)
    public ResponseEntity<ApiError> handleApiError(DockerApiException e) {
        log.warn("Docker-API-Fehler: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("docker-error", e.getMessage()));
    }
}
