package io.d4y.app;

import io.d4y.adapter.compose.ComposeExecutor;
import io.d4y.adapter.compose.ComposeObserver;
import io.d4y.adapter.compose.ComposeOverrideGenerator;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.AppProject;
import io.d4y.domain.model.AuditEntry;
import io.d4y.domain.model.ComposeProject;
import io.d4y.domain.model.ContainerDetails;
import io.d4y.domain.model.ExecResult;
import io.d4y.domain.model.Hold;
import io.d4y.domain.model.HoldType;
import io.d4y.port.AppProjectSource;
import io.d4y.port.ContainerBackend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Führt operative Aktionen aus (ADR-0012/0013) im Compose-Modell (ADR-0029): Lifecycle-Nudges,
 * temporäre Parameter, Inspektion und Hold-Verwaltung. Container-Aktionen laufen über den
 * {@link ContainerBackend} auf der Container-ID des Ziel-Service; temporäre Parameter re-deployen das
 * Compose-Projekt mit zusätzlicher Env. Setzt Holds <b>vor</b> mutierenden Schritten; ändert nie den
 * Sollzustand.
 */
@Service
public class OperationsService {

    private final ContainerBackend backend;
    private final ComposeObserver observer;
    private final AppProjectSource projectSource;
    private final ComposeOverrideGenerator overrides;
    private final ComposeExecutor executor;
    private final SecretStore secretStore;
    private final HoldRegistry holds;
    private final AuditLog audit;
    private final int defaultTail;
    private final String prefix;

    public OperationsService(ContainerBackend backend, ComposeObserver observer,
                             AppProjectSource projectSource, ComposeOverrideGenerator overrides,
                             ComposeExecutor executor, SecretStore secretStore,
                             HoldRegistry holds, AuditLog audit, D4yProperties properties,
                             @Value("${d4y.compose.project-prefix:d4y-}") String prefix) {
        this.backend = backend;
        this.observer = observer;
        this.projectSource = projectSource;
        this.overrides = overrides;
        this.executor = executor;
        this.secretStore = secretStore;
        this.holds = holds;
        this.audit = audit;
        this.defaultTail = properties.operations().logs().defaultTail();
        this.prefix = prefix;
    }

    public void restart(String app, String actor) {
        String id = requireContainerId(app);
        try {
            backend.restart(id);
            audit.record(actor, app, "restart", "OK", false, null);
        } catch (RuntimeException e) {
            audit.record(actor, app, "restart", "FEHLER", false, null);
            throw e;
        }
    }

    public Hold stop(String app, long durationSeconds, String actor) {
        String id = requireContainerId(app);
        Hold hold = holds.set(app, HoldType.STOP, durationSeconds); // zuerst halten
        backend.stop(id);
        audit.record(actor, app, "stop", "OK", true, holdInfo(hold));
        return hold;
    }

    public Hold tempParams(String app, Map<String, String> env, long durationSeconds, String actor) {
        AppProject project = requireDesired(app);
        Hold hold = holds.set(app, HoldType.TEMP_PARAM, durationSeconds); // zuerst halten
        Map<String, String> merged = new HashMap<>(secretStore.all());
        merged.putAll(env);
        Path override = overrides.generate(project);
        executor.up(project.projectName(prefix), project.directory(), project.composeFile(),
                override, false, merged);
        audit.record(actor, app, "temp-param", "OK", true, holdInfo(hold));
        return hold;
    }

    public String logs(String app, Integer tail) {
        return backend.logs(requireContainerId(app), tail == null ? defaultTail : tail);
    }

    public ContainerDetails inspect(String app) {
        return backend.inspect(requireContainerId(app));
    }

    public ExecResult exec(String app, List<String> cmd, String actor) {
        ExecResult result = backend.exec(requireContainerId(app), cmd);
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

    /** Container-ID des Ziel-Service der App (erster Service des Compose-Projekts). */
    private String requireContainerId(String app) {
        String project = prefix + app;
        return observer.observe(prefix).stream()
                .filter(p -> p.name().equals(project))
                .map(ComposeProject::primaryContainerId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElseThrow(() -> new AppNotFoundException("Kein laufender Container für App: " + app));
    }

    private AppProject requireDesired(String app) {
        return projectSource.load().stream()
                .filter(p -> p.name().equals(app))
                .findFirst()
                .orElseThrow(() -> new AppNotFoundException("App nicht deklariert: " + app));
    }

    private String holdInfo(Hold hold) {
        Instant now = holds.now();
        return hold.type() + " · Rest " + hold.remainingSeconds(now) + "s";
    }
}
