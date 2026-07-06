package io.d4y.api;

import io.d4y.api.dto.StatusResponse;
import io.d4y.api.dto.StatusResponse.AppStatus;
import io.d4y.api.dto.StatusResponse.ExtraContainer;
import io.d4y.domain.model.Application;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.ObservedContainer;
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

    public StatusController(DesiredStateSource desiredStateSource, ContainerBackend backend) {
        this.desiredStateSource = desiredStateSource;
        this.backend = backend;
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
            apps.add(new AppStatus(
                    app.name(),
                    app.image().reference(),
                    state,
                    o != null && o.running(),
                    o != null ? o.id() : null));
        }

        List<ExtraContainer> undeclared = new ArrayList<>();
        for (ObservedContainer o : actual) {
            if (!desiredNames.contains(o.appName())) {
                undeclared.add(new ExtraContainer(o.appName(), o.image().reference(), o.id()));
                drift = true;
            }
        }

        return new StatusResponse(drift ? "DRIFT" : "IN_SYNC", apps, undeclared);
    }
}
