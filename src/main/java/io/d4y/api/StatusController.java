package io.d4y.api;

import io.d4y.adapter.compose.Sidecar;
import io.d4y.adapter.docker.DockerEdgeProxy;
import io.d4y.api.dto.StatusResponse;
import io.d4y.api.dto.StatusResponse.AppStatus;
import io.d4y.api.dto.StatusResponse.ExtraProject;
import io.d4y.api.dto.StatusResponse.HoldInfo;
import io.d4y.api.dto.StatusResponse.RouteInfo;
import io.d4y.api.dto.StatusResponse.ServiceStatus;
import io.d4y.app.HoldRegistry;
import io.d4y.domain.model.AppProject;
import io.d4y.domain.model.ComposeProject;
import io.d4y.domain.model.Hold;
import io.d4y.port.AppProjectSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Liefert den aktuellen Plattformzustand (Soll = Compose-App-Projekte vs. Ist). Bewusst
 * <b>read-only</b> — Infrastrukturänderungen erfolgen ausschließlich über das Config-Repository
 * (ADR-0001).
 */
@RestController
@RequestMapping("/api")
public class StatusController {

    private final AppProjectSource projectSource;
    private final io.d4y.adapter.compose.ComposeObserver observer;
    private final HoldRegistry holdRegistry;
    private final DockerEdgeProxy edgeProxy;
    private final String prefix;

    public StatusController(AppProjectSource projectSource,
                            io.d4y.adapter.compose.ComposeObserver observer,
                            HoldRegistry holdRegistry,
                            DockerEdgeProxy edgeProxy,
                            @Value("${d4y.compose.project-prefix:d4y-}") String prefix) {
        this.projectSource = projectSource;
        this.observer = observer;
        this.holdRegistry = holdRegistry;
        this.edgeProxy = edgeProxy;
        this.prefix = prefix;
    }

    @GetMapping("/status")
    public StatusResponse status() {
        List<AppProject> desired = projectSource.load();
        Map<String, ComposeProject> byName = observer.observe(prefix).stream()
                .collect(Collectors.toMap(ComposeProject::name, p -> p, (a, b) -> a, LinkedHashMap::new));
        boolean tlsDefault = edgeProxy.defaultTlsEnabled();

        List<AppStatus> apps = new ArrayList<>();
        Set<String> desiredProjects = new java.util.HashSet<>();
        boolean drift = false;

        for (AppProject app : desired) {
            String project = app.projectName(prefix);
            desiredProjects.add(project);
            ComposeProject observed = byName.get(project);
            String state = observed == null ? "MISSING" : observed.state();
            Hold hold = holdRegistry.get(app.name());
            if (!"RUNNING".equals(state) && hold == null) {
                drift = true;
            }
            apps.add(new AppStatus(
                    app.name(),
                    state,
                    hold == null ? null : new HoldInfo(hold.type().name(), hold.remainingSeconds(holdRegistry.now())),
                    services(observed),
                    routes(app, tlsDefault)));
        }

        List<ExtraProject> undeclared = new ArrayList<>();
        for (ComposeProject p : byName.values()) {
            if (!desiredProjects.contains(p.name())) {
                undeclared.add(new ExtraProject(p.name(), services(p)));
                drift = true;
            }
        }

        return new StatusResponse(drift ? "DRIFT" : "IN_SYNC", apps, undeclared);
    }

    private static List<ServiceStatus> services(ComposeProject project) {
        if (project == null) {
            return List.of();
        }
        return project.containers().stream()
                .map(c -> new ServiceStatus(c.service(), c.image(), c.state()))
                .toList();
    }

    private static List<RouteInfo> routes(AppProject app, boolean tlsDefault) {
        return Sidecar.routes(app).stream()
                .map(r -> new RouteInfo(r.host(), r.path(), r.port(),
                        r.tls() != null ? r.tls() : tlsDefault))
                .toList();
    }
}
