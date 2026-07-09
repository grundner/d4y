package io.d4y.api;

import io.d4y.api.dto.StatusResponse;
import io.d4y.api.dto.StatusResponse.AppStatus;
import io.d4y.api.dto.StatusResponse.ExtraContainer;
import io.d4y.api.dto.StatusResponse.HoldInfo;
import io.d4y.api.dto.StatusResponse.RouteInfo;
import io.d4y.api.dto.StatusResponse.VolumeInfo;
import io.d4y.app.HoldRegistry;
import io.d4y.domain.model.Application;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.Hold;
import io.d4y.domain.model.ObservedContainer;
import io.d4y.domain.model.Route;
import io.d4y.domain.model.VolumeMapping;
import io.d4y.port.ContainerBackend;
import io.d4y.port.DesiredStateSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Liefert den aktuellen Plattformzustand (Soll vs. Ist). Bewusst <b>read-only</b> —
 * Infrastrukturänderungen erfolgen ausschließlich über das Config-Repository (ADR-0001).
 */
@RestController
@RequestMapping("/api")
public class StatusController {

    private final DesiredStateSource desiredStateSource;
    private final ContainerBackend backend;
    private final HoldRegistry holdRegistry;

    public StatusController(DesiredStateSource desiredStateSource, ContainerBackend backend,
                           HoldRegistry holdRegistry) {
        this.desiredStateSource = desiredStateSource;
        this.backend = backend;
        this.holdRegistry = holdRegistry;
    }

    @GetMapping("/status")
    public StatusResponse status() {
        DesiredState desired = desiredStateSource.load();
        List<ObservedContainer> actual = backend.observe();
        Map<String, ObservedContainer> byApp = actual.stream()
                .collect(Collectors.toMap(ObservedContainer::appName, Function.identity(), (a, b) -> a));

        List<AppStatus> apps = new ArrayList<>();
        Set<String> desiredNames = new HashSet<>();
        boolean drift = false;

        for (Application app : desired.applications()) {
            desiredNames.add(app.name());
            ObservedContainer o = byApp.get(app.name());
            String state;
            if (o == null) {
                state = "MISSING";
            } else if (!o.running()) {
                state = "STOPPED";
            } else if (!o.image().equals(app.image())) {
                state = "OUTDATED";
            } else {
                state = "IN_SYNC";
            }
            if (!"IN_SYNC".equals(state)) {
                drift = true;
            }
            Hold hold = holdRegistry.get(app.name());
            HoldInfo holdInfo = hold == null
                    ? null
                    : new HoldInfo(hold.type().name(), hold.remainingSeconds(holdRegistry.now()));
            apps.add(new AppStatus(
                    app.name(),
                    app.image().reference(),
                    state,
                    o != null && o.running(),
                    o != null ? o.id() : null,
                    holdInfo,
                    toVolumeInfos(app.volumes()),
                    toRouteInfos(app.routes())));
        }

        List<ExtraContainer> undeclared = new ArrayList<>();
        for (ObservedContainer o : actual) {
            if (!desiredNames.contains(o.appName())) {
                undeclared.add(new ExtraContainer(
                        o.appName(), o.image().reference(), o.id(), o.running(), toVolumeInfos(o.volumes())));
                drift = true;
            }
        }

        return new StatusResponse(drift ? "DRIFT" : "IN_SYNC", apps, undeclared);
    }

    private static List<VolumeInfo> toVolumeInfos(List<VolumeMapping> volumes) {
        return volumes.stream().map(v -> new VolumeInfo(v.name(), v.path())).toList();
    }

    private static List<RouteInfo> toRouteInfos(List<Route> routes) {
        return routes.stream().map(r -> new RouteInfo(r.host(), r.path(), r.port())).toList();
    }
}
