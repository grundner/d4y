package io.d4y.api;

import io.d4y.adapter.git.GitConfigSync;
import io.d4y.app.PushedConfigStore;
import io.d4y.app.ReconciliationLoop;
import io.d4y.app.SecretStore;
import io.d4y.config.D4yProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Push-Trigger (ADR-0023): eine externe Instanz (typisch GitHub Actions beim Config-Push) stößt
 * Reconcile an und liefert optional Image/Container-Secrets (ADR-0024).
 *
 * <p>Geschützt per Bearer-Token gegen {@code d4y.trigger.token} (host/d4y-Credential). Ohne
 * konfiguriertes Token ist der Endpoint deaktiviert (fail-closed → 503). Secret-Werte werden
 * niemals geloggt oder in der Antwort ausgegeben.
 */
@RestController
@RequestMapping("/api")
public class TriggerController {

    private static final Logger log = LoggerFactory.getLogger(TriggerController.class);

    private final PushedConfigStore pushedConfigStore;
    private final SecretStore secretStore;
    private final GitConfigSync gitSync;
    private final ReconciliationLoop reconciliationLoop;
    private final String token;

    public TriggerController(PushedConfigStore pushedConfigStore, SecretStore secretStore,
                             GitConfigSync gitSync, ReconciliationLoop reconciliationLoop,
                             D4yProperties properties) {
        this.pushedConfigStore = pushedConfigStore;
        this.secretStore = secretStore;
        this.gitSync = gitSync;
        this.reconciliationLoop = reconciliationLoop;
        this.token = properties.trigger().token();
    }

    /**
     * Body des Trigger-Calls (ADR-0025): vollständiger Sollzustand (Dateiname → YAML) und vollständiger
     * Secret-Satz (Name → Wert). Beide idempotenter Replace, beide optional.
     */
    public record TriggerRequest(Map<String, String> config, Map<String, String> secrets) {
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Void> reconcile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) TriggerRequest request) {

        if (token == null || token.isBlank()) {
            // Kein Token konfiguriert ⇒ Endpoint deaktiviert (fail-closed).
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!authorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (request != null) {
            if (request.config() != null) {
                try {
                    pushedConfigStore.replaceAll(request.config());
                } catch (IllegalArgumentException e) {
                    // z. B. ungültiger/traversierender Dateiname — kein Secret in der Antwort.
                    return ResponseEntity.badRequest().build();
                }
            }
            if (request.secrets() != null) {
                secretStore.replaceAll(request.secrets());
            }
        }
        // Pull-Modus (ADR-0019) bleibt optional; im Voll-Push-Modus ist gitSync inert.
        if (gitSync.enabled()) {
            gitSync.sync();
        }
        reconciliationLoop.reconcile();
        log.info("Reconcile per Push getriggert");
        return ResponseEntity.accepted().build();
    }

    private boolean authorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        String provided = authorization.substring("Bearer ".length());
        // Konstantzeitiger Vergleich gegen Timing-Angriffe.
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }
}
