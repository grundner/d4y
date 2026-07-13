package io.d4y.app;

import io.d4y.adapter.compose.ComposeExecutor;
import io.d4y.adapter.compose.ComposeOverrideGenerator;
import io.d4y.adapter.docker.DockerEdgeProxy;
import io.d4y.domain.model.AppProject;
import io.d4y.port.AppProjectSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reconciliert den Sollzustand als Compose-Projekte (ADR-0029): je deklarierter App
 * {@code docker compose up -d}, für nicht mehr deklarierte, von d4y verwaltete Projekte
 * {@code docker compose down}. Idempotent — dadurch selbstheilend (ADR-0007).
 *
 * <p>Fehler einer einzelnen App blockieren die übrigen nicht; sie werden geloggt und beim nächsten
 * Durchlauf erneut versucht.
 */
@Component
public class ComposeReconciler {

    private static final Logger log = LoggerFactory.getLogger(ComposeReconciler.class);

    private final AppProjectSource source;
    private final ComposeOverrideGenerator overrides;
    private final ComposeExecutor executor;
    private final DockerEdgeProxy edgeProxy;
    private final SecretStore secretStore;
    private final String prefix;

    public ComposeReconciler(AppProjectSource source,
                             ComposeOverrideGenerator overrides,
                             ComposeExecutor executor,
                             DockerEdgeProxy edgeProxy,
                             SecretStore secretStore,
                             @Value("${d4y.compose.project-prefix:d4y-}") String prefix) {
        this.source = source;
        this.overrides = overrides;
        this.executor = executor;
        this.edgeProxy = edgeProxy;
        this.secretStore = secretStore;
        this.prefix = prefix;
    }

    /**
     * Ein Reconcile-Durchlauf.
     *
     * @param heldAppNames Apps mit aktivem Reconciliation-Hold (werden weder gestartet noch entfernt)
     * @param build        {@code true} ⇒ {@code up --build} (Push-getriggert); sonst reines {@code up}
     */
    public void reconcile(Set<String> heldAppNames, boolean build) {
        edgeProxy.ensureNetwork(); // externes d4y-Netz muss existieren, bevor das Override es referenziert

        // Gelieferte Secrets als Prozess-Env für die Compose-Interpolation (${VAR}); nie auf Platte.
        Map<String, String> secretEnv = secretStore.all();

        List<AppProject> desired = source.load();
        Set<String> desiredProjects = new HashSet<>();
        for (AppProject app : desired) {
            String project = app.projectName(prefix);
            desiredProjects.add(project);
            if (heldAppNames.contains(app.name())) {
                continue;
            }
            try {
                Path override = overrides.generate(app);
                executor.up(project, app.directory(), app.composeFile(), override, build, secretEnv);
            } catch (RuntimeException e) {
                log.warn("App '{}' konnte nicht reconciled werden: {}", app.name(), e.getMessage());
                log.debug("Details", e);
            }
        }

        // Drift: verwaltete Compose-Projekte ohne Deklaration entfernen.
        for (String project : executor.listProjects()) {
            if (!project.startsWith(prefix) || desiredProjects.contains(project)) {
                continue;
            }
            String appName = project.substring(prefix.length());
            if (heldAppNames.contains(appName)) {
                continue;
            }
            try {
                log.info("Undeklariertes Projekt '{}' wird entfernt", project);
                executor.down(project);
            } catch (RuntimeException e) {
                log.warn("Projekt '{}' konnte nicht entfernt werden: {}", project, e.getMessage());
            }
        }
    }
}
