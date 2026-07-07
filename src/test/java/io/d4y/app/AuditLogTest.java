package io.d4y.app;

import io.d4y.domain.model.AuditEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogTest {

    @Test
    void newestFirstAndLimited() {
        AuditLog log = new AuditLog();
        log.record("alice", "web", "restart", "OK", false, null);
        log.record("bob", "db", "stop", "OK", true, "STOP");

        assertThat(log.recent(10)).extracting(AuditEntry::action).containsExactly("stop", "restart");
        assertThat(log.recent(1)).hasSize(1);
        assertThat(log.recent(1).get(0).app()).isEqualTo("db");
    }

    @Test
    void cappedAtCapacity() {
        AuditLog log = new AuditLog();
        for (int i = 0; i < 600; i++) {
            log.record("system", "app" + i, "inspect", "OK", false, null);
        }
        assertThat(log.recent(0)).hasSize(500);
        // Der jüngste Eintrag ist app599.
        assertThat(log.recent(1).get(0).app()).isEqualTo("app599");
    }
}
