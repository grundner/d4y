package io.d4y.app;

import io.d4y.domain.model.Application;
import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.ImageRef;
import io.d4y.domain.model.ObservedContainer;
import io.d4y.domain.reconcile.Reconciler;
import io.d4y.port.ContainerBackend;
import io.d4y.port.DesiredStateSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationLoopTest {

    /** In-Memory-Backend, das das Verhalten einer Engine nachbildet. */
    static class FakeBackend implements ContainerBackend {
        final List<ObservedContainer> containers = new ArrayList<>();
        final AtomicInteger runCount = new AtomicInteger();
        private int seq;

        @Override
        public List<ObservedContainer> observe() {
            return List.copyOf(containers);
        }

        @Override
        public void ensureImage(ImageRef image) {
            // no-op
        }

        @Override
        public String run(ContainerSpec spec) {
            String id = "c" + (++seq);
            containers.add(new ObservedContainer(id, spec.appName(), spec.image(), true));
            runCount.incrementAndGet();
            return id;
        }

        @Override
        public void stopAndRemove(String containerId) {
            containers.removeIf(c -> c.id().equals(containerId));
        }

        void crash(String appName) {
            containers.removeIf(c -> c.appName().equals(appName));
        }
    }

    static class MutableSource implements DesiredStateSource {
        DesiredState state;

        MutableSource(DesiredState state) {
            this.state = state;
        }

        @Override
        public DesiredState load() {
            return state;
        }
    }

    private static DesiredState desired(String name, String image) {
        return new DesiredState(List.of(new Application(name, ImageRef.of(image))));
    }

    @Test
    void createsThenStaysIdempotentThenSelfHealsThenCleansDrift() {
        FakeBackend backend = new FakeBackend();
        MutableSource source = new MutableSource(desired("web", "nginx:1.27"));
        ReconciliationLoop loop = new ReconciliationLoop(source, backend, new Reconciler());

        // 1) Erstlauf: App wird angelegt.
        loop.reconcile();
        assertThat(backend.containers).hasSize(1);
        assertThat(backend.runCount.get()).isEqualTo(1);

        // 2) Zweitlauf: bereits in Sync → idempotent, kein weiterer run.
        loop.reconcile();
        assertThat(backend.runCount.get()).isEqualTo(1);

        // 3) Self-Healing: Container "stürzt ab" → nächster Lauf legt ihn neu an.
        backend.crash("web");
        loop.reconcile();
        assertThat(backend.containers).hasSize(1);
        assertThat(backend.runCount.get()).isEqualTo(2);

        // 4) Drift-Bereinigung: App aus dem Soll entfernt → Container wird entfernt.
        source.state = DesiredState.empty();
        loop.reconcile();
        assertThat(backend.containers).isEmpty();
    }
}
