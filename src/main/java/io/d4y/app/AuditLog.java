package io.d4y.app;

import io.d4y.domain.model.AuditEntry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * In-Memory-Audit-Log operativer Aktionen (ADR-0013), als gekappter Ring-Puffer.
 * Thread-sicher; neueste Einträge zuerst.
 */
@Component
public class AuditLog {

    private static final int CAPACITY = 500;

    private final Deque<AuditEntry> entries = new ArrayDeque<>();
    private final Clock clock;

    public AuditLog() {
        this(Clock.systemUTC());
    }

    AuditLog(Clock clock) {
        this.clock = clock;
    }

    public synchronized void record(String actor, String app, String action, String result,
                                    boolean drift, String holdInfo) {
        entries.addFirst(new AuditEntry(clock.instant(), actor, app, action, result, drift, holdInfo));
        while (entries.size() > CAPACITY) {
            entries.removeLast();
        }
    }

    /** Die jüngsten {@code limit} Einträge (neueste zuerst). */
    public synchronized List<AuditEntry> recent(int limit) {
        int max = limit <= 0 ? entries.size() : Math.min(limit, entries.size());
        List<AuditEntry> out = new ArrayList<>(max);
        Iterator<AuditEntry> it = entries.iterator();
        while (it.hasNext() && out.size() < max) {
            out.add(it.next());
        }
        return out;
    }
}
