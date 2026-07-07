package io.d4y.app;

import io.d4y.TestFixtures;
import io.d4y.domain.model.Application;
import io.d4y.domain.model.ContainerDetails;
import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.ExecResult;
import io.d4y.domain.model.HoldType;
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

        @Override
        public void restart(String containerId) {
        }

        @Override
        public void stop(String containerId) {
            containers.replaceAll(c -> c.id().equals(containerId)
                    ? new ObservedContainer(c.id(), c.appName(), c.image(), false) : c);
        }

        @Override
        public String logs(String containerId, int tail) {
            return "";
        }

        @Override
        public ContainerDetails inspect(String containerId) {
            return new ContainerDetails(containerId, "", "", "", "", "", List.of());
        }

        @Override
        public ExecResult exec(String containerId, List<String> cmd) {
            return new ExecResult("", 0);
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

    private ReconciliationLoop loop(DesiredStateSource source, ContainerBackend backend, HoldRegistry holds) {
        return new ReconciliationLoop(source, backend, new Reconciler(), holds, new AuditLog());
    }

    @Test
    void createsThenStaysIdempotentThenSelfHealsThenCleansDrift() {
        FakeBackend backend = new FakeBackend();
        MutableSource source = new MutableSource(desired("web", "nginx:1.27"));
        HoldRegistry holds = new HoldRegistry(TestFixtures.props());
        ReconciliationLoop loop = loop(source, backend, holds);

        loop.reconcile();
        assertThat(backend.containers).hasSize(1);
        assertThat(backend.runCount.get()).isEqualTo(1);

        loop.reconcile();
        assertThat(backend.runCount.get()).isEqualTo(1);

        backend.crash("web");
        loop.reconcile();
        assertThat(backend.containers).hasSize(1);
        assertThat(backend.runCount.get()).isEqualTo(2);

        source.state = DesiredState.empty();
        loop.reconcile();
        assertThat(backend.containers).isEmpty();
    }

    @Test
    void heldAppIsNotReconciled() {
        FakeBackend backend = new FakeBackend();
        MutableSource source = new MutableSource(desired("web", "nginx:1.27"));
        HoldRegistry holds = new HoldRegistry(TestFixtures.props());
        ReconciliationLoop loop = loop(source, backend, holds);

        loop.reconcile();
        assertThat(backend.containers).hasSize(1);

        // Gehalten + abgestürzt: der Loop stellt NICHT wieder her.
        holds.set("web", HoldType.STOP, 300);
        backend.crash("web");
        loop.reconcile();
        assertThat(backend.containers).isEmpty();
        assertThat(backend.runCount.get()).isEqualTo(1);

        // Nach Freigabe heilt der Loop wieder.
        holds.release("web");
        loop.reconcile();
        assertThat(backend.containers).hasSize(1);
        assertThat(backend.runCount.get()).isEqualTo(2);
    }
}
