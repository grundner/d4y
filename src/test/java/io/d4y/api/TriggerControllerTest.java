package io.d4y.api;

import io.d4y.TestFixtures;
import io.d4y.adapter.git.GitConfigSync;
import io.d4y.app.ReconciliationLoop;
import io.d4y.app.SecretStore;
import io.d4y.config.D4yProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TriggerControllerTest {

    private final SecretStore secretStore = mock(SecretStore.class);
    private final GitConfigSync gitSync = mock(GitConfigSync.class);
    private final ReconciliationLoop loop = mock(ReconciliationLoop.class);

    private TriggerController controller(String token) {
        D4yProperties props = TestFixtures.props("./desired",
                new D4yProperties.Trigger(token),
                new D4yProperties.Secrets("", "./.d4y-secrets"));
        return new TriggerController(secretStore, gitSync, loop, props);
    }

    @Test
    void disabledWhenNoTokenConfigured() {
        ResponseEntity<Void> res = controller("").reconcile("Bearer whatever", null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(loop, never()).reconcile();
    }

    @Test
    void rejectsMissingOrWrongToken() {
        TriggerController c = controller("s3kret");
        assertThat(c.reconcile(null, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(c.reconcile("Bearer falsch", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(secretStore, never()).replaceAll(org.mockito.ArgumentMatchers.any());
        verify(loop, never()).reconcile();
    }

    @Test
    void acceptsCorrectTokenStoresSecretsAndTriggersReconcile() {
        var request = new TriggerController.TriggerRequest(Map.of("GHCR_TOKEN", "abc"));
        ResponseEntity<Void> res = controller("s3kret").reconcile("Bearer s3kret", request);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(secretStore).replaceAll(Map.of("GHCR_TOKEN", "abc"));
        verify(loop).reconcile();
    }
}
