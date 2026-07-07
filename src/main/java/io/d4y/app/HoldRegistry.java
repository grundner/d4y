package io.d4y.app;

import io.d4y.config.D4yProperties;
import io.d4y.domain.model.Hold;
import io.d4y.domain.model.HoldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory-Registry aktiver {@link Hold}s (ADR-0013). Holds sind transient und zeitlich
 * begrenzt; ein Server-Neustart kehrt zu reinem GitOps zurück.
 */
@Component
public class HoldRegistry {

    private final Map<String, Hold> holds = new ConcurrentHashMap<>();
    private final long defaultSeconds;
    private final long maxSeconds;
    private final Clock clock;

    @Autowired
    public HoldRegistry(D4yProperties properties) {
        this(properties, Clock.systemUTC());
    }

    HoldRegistry(D4yProperties properties, Clock clock) {
        this.defaultSeconds = properties.operations().hold().defaultSeconds();
        this.maxSeconds = properties.operations().hold().maxSeconds();
        this.clock = clock;
    }

    /** Setzt einen Hold. Dauer wird auf [1, max] geklemmt; {@code <= 0} nutzt den Default. */
    public Hold set(String appName, HoldType type, long durationSeconds) {
        long dur = durationSeconds <= 0 ? defaultSeconds : durationSeconds;
        dur = Math.max(1, Math.min(dur, maxSeconds));
        Hold hold = new Hold(appName, type, clock.instant().plusSeconds(dur));
        holds.put(appName, hold);
        return hold;
    }

    /** Gibt einen Hold frei; liefert {@code true}, wenn einer aktiv war. */
    public boolean release(String appName) {
        return holds.remove(appName) != null;
    }

    /** Aktiver Hold für die App oder {@code null} (abgelaufene werden verworfen). */
    public Hold get(String appName) {
        Hold h = holds.get(appName);
        if (h == null) {
            return null;
        }
        if (h.isExpired(clock.instant())) {
            holds.remove(appName, h);
            return null;
        }
        return h;
    }

    public boolean isHeld(String appName) {
        return get(appName) != null;
    }

    public Set<String> heldAppNames() {
        return Set.copyOf(activeMap().keySet());
    }

    public List<Hold> active() {
        return new ArrayList<>(activeMap().values());
    }

    /** Entfernt abgelaufene Holds und liefert sie (für das Auditieren von {@code hold-expired}). */
    public List<Hold> purgeExpired() {
        Instant now = clock.instant();
        List<Hold> expired = new ArrayList<>();
        for (Hold h : holds.values()) {
            if (h.isExpired(now) && holds.remove(h.appName(), h)) {
                expired.add(h);
            }
        }
        return expired;
    }

    public Instant now() {
        return clock.instant();
    }

    private Map<String, Hold> activeMap() {
        Instant now = clock.instant();
        Map<String, Hold> out = new ConcurrentHashMap<>();
        for (Hold h : holds.values()) {
            if (h.isExpired(now)) {
                holds.remove(h.appName(), h);
            } else {
                out.put(h.appName(), h);
            }
        }
        return out;
    }
}
