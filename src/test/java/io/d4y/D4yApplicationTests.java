package io.d4y;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Lädt den vollständigen Spring-Kontext und fängt Verdrahtungsfehler ab. Ein sehr großes
 * Reconcile-Intervall verhindert, dass der Loop während des Tests die Engine anspricht.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "d4y.reconcile.interval-ms=3600000",
        "d4y.desired-state.path=./nonexistent-test-desired"
})
class D4yApplicationTests {

    @Test
    void contextLoads() {
    }
}
