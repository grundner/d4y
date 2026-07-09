package io.d4y.domain.reconcile;

import io.d4y.domain.model.Application;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.ImageRef;
import io.d4y.domain.model.ObservedContainer;
import io.d4y.domain.model.VolumeMapping;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReconcilerTest {

    private final Reconciler reconciler = new Reconciler();

    private static Application app(String name, String image) {
        return new Application(name, ImageRef.of(image));
    }

    private static ObservedContainer container(String id, String app, String image, boolean running) {
        return new ObservedContainer(id, app, ImageRef.of(image), running);
    }

    @Test
    void startsMissingApplication() {
        var plan = reconciler.plan(new DesiredState(List.of(app("web", "nginx:1.27"))), List.of());

        assertThat(plan.actions()).singleElement()
                .isInstanceOfSatisfying(ReconcileAction.Start.class,
                        s -> assertThat(s.application().name()).isEqualTo("web"));
        assertThat(plan.isInSync()).isFalse();
    }

    @Test
    void noopWhenRunningAndImageMatches() {
        var plan = reconciler.plan(
                new DesiredState(List.of(app("web", "nginx:1.27"))),
                List.of(container("c1", "web", "nginx:1.27", true)));

        assertThat(plan.actions()).singleElement().isInstanceOf(ReconcileAction.Noop.class);
        assertThat(plan.isInSync()).isTrue();
    }

    @Test
    void replacesStoppedContainer() {
        var plan = reconciler.plan(
                new DesiredState(List.of(app("web", "nginx:1.27"))),
                List.of(container("c1", "web", "nginx:1.27", false)));

        assertThat(plan.actions()).singleElement()
                .isInstanceOfSatisfying(ReconcileAction.Replace.class,
                        r -> assertThat(r.currentContainerId()).isEqualTo("c1"));
    }

    @Test
    void replacesOnImageChange() {
        var plan = reconciler.plan(
                new DesiredState(List.of(app("web", "nginx:1.28"))),
                List.of(container("c1", "web", "nginx:1.27", true)));

        assertThat(plan.actions()).singleElement().isInstanceOf(ReconcileAction.Replace.class);
    }

    @Test
    void replacesOnVolumeChange() {
        Application desired = new Application("web", ImageRef.of("nginx:1.27"),
                List.of(new VolumeMapping("html", "/usr/share/nginx/html")));
        ObservedContainer observed = new ObservedContainer("c1", "web", ImageRef.of("nginx:1.27"), true,
                List.of(new VolumeMapping("html", "/other/path")));

        var plan = reconciler.plan(new DesiredState(List.of(desired)), List.of(observed));

        assertThat(plan.actions()).singleElement().isInstanceOf(ReconcileAction.Replace.class);
    }

    @Test
    void noopWhenVolumesMatch() {
        Application desired = new Application("web", ImageRef.of("nginx:1.27"),
                List.of(new VolumeMapping("html", "/usr/share/nginx/html")));
        ObservedContainer observed = new ObservedContainer("c1", "web", ImageRef.of("nginx:1.27"), true,
                List.of(new VolumeMapping("html", "/usr/share/nginx/html")));

        var plan = reconciler.plan(new DesiredState(List.of(desired)), List.of(observed));

        assertThat(plan.actions()).singleElement().isInstanceOf(ReconcileAction.Noop.class);
        assertThat(plan.isInSync()).isTrue();
    }

    @Test
    void removesUndeclaredManagedContainer() {
        var plan = reconciler.plan(
                DesiredState.empty(),
                List.of(container("c9", "orphan", "redis:7", true)));

        assertThat(plan.actions()).singleElement()
                .isInstanceOfSatisfying(ReconcileAction.StopAndRemove.class,
                        r -> assertThat(r.containerId()).isEqualTo("c9"));
    }

    @Test
    void emptyDesiredAndActualIsInSync() {
        var plan = reconciler.plan(DesiredState.empty(), List.of());

        assertThat(plan.actions()).isEmpty();
        assertThat(plan.isInSync()).isTrue();
    }

    @Test
    void skipsHeldDesiredApp() {
        var plan = reconciler.plan(
                new DesiredState(List.of(app("web", "nginx:1.27"))), List.of(), Set.of("web"));

        assertThat(plan.actions()).singleElement().isInstanceOf(ReconcileAction.Noop.class);
    }

    @Test
    void doesNotRemoveHeldUndeclaredContainer() {
        var plan = reconciler.plan(
                DesiredState.empty(),
                List.of(container("c9", "orphan", "redis:7", true)),
                Set.of("orphan"));

        assertThat(plan.actions()).isEmpty();
    }
}
