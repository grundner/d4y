package io.d4y.app;

import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.Hold;
import io.d4y.domain.model.ObservedContainer;
import io.d4y.domain.reconcile.ReconcileAction;
import io.d4y.domain.reconcile.ReconcilePlan;
import io.d4y.domain.reconcile.Reconciler;
import io.d4y.port.ContainerBackend;
import io.d4y.port.DesiredStateSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Kontinuierlicher Reconciliation-Loop (ADR-0007): observe → diff → reconcile.
 *
 * <p>Läuft periodisch. Ein fehlgeschlagener Durchlauf blockiert den Loop nicht dauerhaft —
 * beim nächsten Intervall wird erneut versucht. {@code fixedDelay} serialisiert die Läufe.
 */
@Component
public class ReconciliationLoop {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationLoop.class);

    private final DesiredStateSource desiredStateSource;
    private final ContainerBackend backend;
    private final Reconciler reconciler;
    private final HoldRegistry holdRegistry;
    private final AuditLog auditLog;

    public ReconciliationLoop(DesiredStateSource desiredStateSource,
                              ContainerBackend backend,
                              Reconciler reconciler,
                              HoldRegistry holdRegistry,
                              AuditLog auditLog) {
        this.desiredStateSource = desiredStateSource;
        this.backend = backend;
        this.reconciler = reconciler;
        this.holdRegistry = holdRegistry;
        this.auditLog = auditLog;
    }

    @Scheduled(fixedDelayString = "${d4y.reconcile.interval-ms}")
    public void reconcile() {
        try {
            // Abgelaufene Holds bereinigen und auditieren; danach kehren die Ziele zu GitOps zurück.
            for (Hold expired : holdRegistry.purgeExpired()) {
                auditLog.record("system", expired.appName(), "hold-expired", "OK", false, "abgelaufen");
            }
            Set<String> held = holdRegistry.heldAppNames();

            DesiredState desired = desiredStateSource.load();
            List<ObservedContainer> actual = backend.observe();
            ReconcilePlan plan = reconciler.plan(desired, actual, held);

            if (plan.isInSync()) {
                log.debug("In Sync — {} App(s), {} gehalten", desired.applications().size(), held.size());
                return;
            }
            log.info("Drift erkannt — {} Aktion(en) werden ausgeführt", plan.changingActions().size());
            plan.actions().forEach(this::apply);
        } catch (Exception e) {
            log.warn("Reconciliation-Durchlauf fehlgeschlagen, erneuter Versuch beim nächsten Intervall: {}",
                    e.getMessage());
            log.debug("Details", e);
        }
    }

    private void apply(ReconcileAction action) {
        switch (action) {
            case ReconcileAction.Start start ->
                    backend.run(ContainerSpec.forApplication(start.application()));
            case ReconcileAction.Replace replace -> {
                backend.stopAndRemove(replace.currentContainerId());
                backend.run(ContainerSpec.forApplication(replace.application()));
            }
            case ReconcileAction.StopAndRemove remove ->
                    backend.stopAndRemove(remove.containerId());
            case ReconcileAction.Noop ignored -> {
                // nichts zu tun
            }
        }
    }
}
