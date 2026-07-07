package io.d4y.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Zentrale Konfiguration von D4Y (Präfix {@code d4y}).
 *
 * <p>Server sind austauschbar und tragen keine individuelle Konfiguration; diese Werte
 * beschreiben lediglich, wie das Backend mit der Engine und der Desired-State-Quelle spricht.
 */
@ConfigurationProperties(prefix = "d4y")
public record D4yProperties(
        @DefaultValue Docker docker,
        @DefaultValue DesiredState desiredState,
        @DefaultValue Reconcile reconcile,
        @DefaultValue Operations operations) {

    /** Anbindung an die Docker-Engine (HTTP über Unix-Socket). */
    public record Docker(
            @DefaultValue("/var/run/docker.sock") String socketPath,
            /** Optionaler API-Versions-Präfix, z. B. {@code /v1.43}. Leer = Engine-Default. */
            @DefaultValue("") String apiVersion) {
    }

    /** Herkunft des Sollzustands. ADR-0011 (Interim): lokales Verzeichnis, später Git. */
    public record DesiredState(@DefaultValue("./desired") String path) {
    }

    /** Steuerung des Reconciliation-Loops. */
    public record Reconcile(@DefaultValue("15000") long intervalMs) {
    }

    /** Operative Aktionen und Hold (ADR-0013). */
    public record Operations(@DefaultValue HoldConfig hold, @DefaultValue LogsConfig logs) {
    }

    public record HoldConfig(
            @DefaultValue("900") long defaultSeconds,
            @DefaultValue("3600") long maxSeconds) {
    }

    public record LogsConfig(@DefaultValue("200") int defaultTail) {
    }
}
