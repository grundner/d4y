package io.d4y.app;

import io.d4y.config.D4yProperties;
import io.d4y.domain.model.Application;
import io.d4y.domain.model.AuditEntry;
import io.d4y.domain.model.ContainerDetails;
import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.ExecResult;
import io.d4y.domain.model.Hold;
import io.d4y.domain.model.HoldType;
import io.d4y.domain.model.ObservedContainer;
import io.d4y.port.ContainerBackend;
import io.d4y.port.DesiredStateSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Führt operative Aktionen aus (ADR-0012/0013): Lifecycle-Nudges, temporäre Parameter,
 * Inspektion sowie Hold-Verwaltung. Setzt Holds <b>vor</b> mutierenden Schritten, damit der
 * Reconciler nicht dazwischen greift. Ändert nie den Sollzustand.
 */
@Service
public class OperationsService {

    private final ContainerBackend backend;
    private final DesiredStateSource desiredStateSource;
    private final HoldRegistry holds;
    private final AuditLog audit;
    private final int defaultTail;

    public OperationsService(ContainerBackend backend, DesiredStateSource desiredStateSource,
                             HoldRegistry holds, AuditLog audit, D4yProperties properties) {
        this.backend = backend;
        this.desiredStateSource = desiredStateSource;
        this.holds = holds;
        this.audit = audit;
        this.defaultTail = properties.operations().logs().defaultTail();
    }

    public void restart(String app, String actor) {
        ObservedContainer c = requireContainer(app);
        try {
            backend.restart(c.id());
            audit.record(actor, app, "restart", "OK", false, null);
        } catch (RuntimeException e) {
            audit.record(actor, app, "restart", "FEHLER", false, null);
            throw e;
        }
    }

    public Hold stop(String app, long durationSeconds, String actor) {
        ObservedContainer c = requireContainer(app);
        Hold hold = holds.set(app, HoldType.STOP, durationSeconds); // zuerst halten
        backend.stop(c.id());
        audit.record(actor, app, "stop", "OK", true, holdInfo(hold));
        return hold;
    }

    public Hold tempParams(String app, Map<String, String> env, long durationSeconds, String actor) {
        Application a = requireDesired(app);
        Hold hold = holds.set(app, HoldType.TEMP_PARAM, durationSeconds); // zuerst halten
        backend.observe().stream()
                .filter(c -> c.appName().equals(app))
                .findFirst()
                .ifPresent(c -> backend.stopAndRemove(c.id()));
        backend.run(ContainerSpec.forApplication(a, env));
        audit.record(actor, app, "temp-param", "OK", true, holdInfo(hold));
        return hold;
    }

    public String logs(String app, Integer tail) {
        ObservedContainer c = requireContainer(app);
        return backend.logs(c.id(), tail == null ? defaultTail : tail);
    }

    public ContainerDetails inspect(String app) {
        return backend.inspect(requireContainer(app).id());
    }

    public ExecResult exec(String app, List<String> cmd, String actor) {
        ObservedContainer c = requireContainer(app);
        ExecResult result = backend.exec(c.id(), cmd);
        audit.record(actor, app, "exec", result.exitCode() == 0 ? "OK" : "FEHLER", true, null);
        return result;
    }

    public Hold setHold(String app, HoldType type, long durationSeconds, String actor) {
        Hold hold = holds.set(app, type, durationSeconds);
        audit.record(actor, app, "hold-set", "OK", true, holdInfo(hold));
        return hold;
    }

    public boolean releaseHold(String app, String actor) {
        boolean was = holds.release(app);
        if (was) {
            audit.record(actor, app, "hold-released", "OK", false, null);
        }
        return was;
    }

    public List<Hold> activeHolds() {
        return holds.active();
    }

    public long remainingSeconds(Hold hold) {
        return hold.remainingSeconds(holds.now());
    }

    public List<AuditEntry> activity(int limit) {
        return audit.recent(limit);
    }

    // --- intern --------------------------------------------------------------------------

    private ObservedContainer requireContainer(String app) {
        return backend.observe().stream()
                .filter(c -> c.appName().equals(app))
                .findFirst()
                .orElseThrow(() -> new AppNotFoundException("Kein laufender Container für App: " + app));
    }

    private Application requireDesired(String app) {
        return desiredStateSource.load().applications().stream()
                .filter(a -> a.name().equals(app))
                .findFirst()
                .orElseThrow(() -> new AppNotFoundException("App nicht deklariert: " + app));
    }

    private String holdInfo(Hold hold) {
        Instant now = holds.now();
        return hold.type() + " · Rest " + hold.remainingSeconds(now) + "s";
    }
}
