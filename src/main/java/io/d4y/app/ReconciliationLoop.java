package io.d4y.app;

import io.d4y.domain.model.Hold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Kontinuierlicher Reconciliation-Loop (ADR-0007): gleicht den Sollzustand als Compose-Projekte an
 * (ADR-0029). Läuft periodisch; ein fehlgeschlagener Durchlauf blockiert den Loop nicht dauerhaft.
 * {@code fixedDelay} serialisiert die Läufe.
 */
@Component
public class ReconciliationLoop {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationLoop.class);

    private final ComposeReconciler composeReconciler;
    private final HoldRegistry holdRegistry;
    private final AuditLog auditLog;

    public ReconciliationLoop(ComposeReconciler composeReconciler,
                              HoldRegistry holdRegistry,
                              AuditLog auditLog) {
        this.composeReconciler = composeReconciler;
        this.holdRegistry = holdRegistry;
        this.auditLog = auditLog;
    }

    @Scheduled(fixedDelayString = "${d4y.reconcile.interval-ms}")
    public void reconcile() {
        reconcile(false);
    }

    /**
     * Ein Reconcile-Durchlauf.
     *
     * @param build {@code true} ⇒ {@code docker compose up --build} (z. B. Push-getriggert)
     */
    public void reconcile(boolean build) {
        try {
            // Abgelaufene Holds bereinigen und auditieren; danach kehren die Ziele zu GitOps zurück.
            for (Hold expired : holdRegistry.purgeExpired()) {
                auditLog.record("system", expired.appName(), "hold-expired", "OK", false, "abgelaufen");
            }
            Set<String> held = holdRegistry.heldAppNames();
            composeReconciler.reconcile(held, build);
        } catch (Exception e) {
            log.warn("Reconciliation-Durchlauf fehlgeschlagen, erneuter Versuch beim nächsten Intervall: {}",
                    e.getMessage());
            log.debug("Details", e);
        }
    }
}
