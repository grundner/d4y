package io.d4y.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Ein zeitlich begrenzter Reconciliation-Hold für ein Ziel (Application).
 *
 * <p>Operativer, transienter Zustand — kein Sollzustand. Läuft automatisch ab.
 */
public record Hold(String appName, HoldType type, Instant expiresAt) {

    public Hold {
        Objects.requireNonNull(appName, "appName");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Verbleibende Sekunden (nie negativ). */
    public long remainingSeconds(Instant now) {
        long s = Duration.between(now, expiresAt).getSeconds();
        return Math.max(0, s);
    }
}
