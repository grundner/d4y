package io.d4y.app;

import io.d4y.TestFixtures;
import io.d4y.domain.model.HoldType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Der Loop (ADR-0029) delegiert je Durchlauf an den {@link ComposeReconciler} und reicht die
 * gehaltenen Apps sowie das Build-Flag durch. Die eigentliche Compose-Logik ist dort getestet.
 */
class ReconciliationLoopTest {

    @Test
    void delegatesToComposeReconcilerWithHeldApps() {
        ComposeReconciler composeReconciler = mock(ComposeReconciler.class);
        HoldRegistry holds = new HoldRegistry(TestFixtures.props());
        ReconciliationLoop loop = new ReconciliationLoop(composeReconciler, holds, new AuditLog());

        loop.reconcile();
        verify(composeReconciler).reconcile(Set.of(), false);

        holds.set("web", HoldType.STOP, 300);
        loop.reconcile();
        verify(composeReconciler).reconcile(Set.of("web"), false);
    }

    @Test
    void reconcileWithBuildPassesBuildTrue() {
        ComposeReconciler composeReconciler = mock(ComposeReconciler.class);
        ReconciliationLoop loop = new ReconciliationLoop(
                composeReconciler, new HoldRegistry(TestFixtures.props()), new AuditLog());

        loop.reconcile(true);
        verify(composeReconciler).reconcile(Set.of(), true);
    }
}
