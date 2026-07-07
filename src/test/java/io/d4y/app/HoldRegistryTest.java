package io.d4y.app;

import io.d4y.TestFixtures;
import io.d4y.domain.model.Hold;
import io.d4y.domain.model.HoldType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class HoldRegistryTest {

    static class MutableClock extends Clock {
        Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private final Instant t0 = Instant.parse("2026-07-07T12:00:00Z");

    @Test
    void setsAndReadsActiveHold() {
        MutableClock clock = new MutableClock(t0);
        HoldRegistry reg = new HoldRegistry(TestFixtures.props(), clock);

        Hold h = reg.set("web", HoldType.STOP, 300);
        assertThat(h.remainingSeconds(clock.instant())).isEqualTo(300);
        assertThat(reg.isHeld("web")).isTrue();
        assertThat(reg.heldAppNames()).containsExactly("web");
    }

    @Test
    void clampsToMaxAndUsesDefault() {
        MutableClock clock = new MutableClock(t0);
        HoldRegistry reg = new HoldRegistry(TestFixtures.props(), clock);

        assertThat(reg.set("a", HoldType.MANUAL, 999_999).remainingSeconds(clock.instant())).isEqualTo(3600);
        assertThat(reg.set("b", HoldType.MANUAL, 0).remainingSeconds(clock.instant())).isEqualTo(900);
    }

    @Test
    void expiresAutomatically() {
        MutableClock clock = new MutableClock(t0);
        HoldRegistry reg = new HoldRegistry(TestFixtures.props(), clock);
        reg.set("web", HoldType.STOP, 300);

        clock.advance(299);
        assertThat(reg.isHeld("web")).isTrue();

        clock.advance(2); // t0+301
        assertThat(reg.get("web")).isNull();
        assertThat(reg.active()).isEmpty();
    }

    @Test
    void purgeExpiredReturnsExpiredHolds() {
        MutableClock clock = new MutableClock(t0);
        HoldRegistry reg = new HoldRegistry(TestFixtures.props(), clock);
        reg.set("web", HoldType.STOP, 60);

        clock.advance(61);
        assertThat(reg.purgeExpired()).extracting(Hold::appName).containsExactly("web");
        assertThat(reg.purgeExpired()).isEmpty();
    }

    @Test
    void releaseRemovesHold() {
        HoldRegistry reg = new HoldRegistry(TestFixtures.props(), new MutableClock(t0));
        reg.set("web", HoldType.STOP, 60);
        assertThat(reg.release("web")).isTrue();
        assertThat(reg.release("web")).isFalse();
        assertThat(reg.isHeld("web")).isFalse();
    }
}
