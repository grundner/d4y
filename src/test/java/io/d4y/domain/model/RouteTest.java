package io.d4y.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteTest {

    @Test
    void defaultsPathToRoot() {
        assertThat(new Route("web.example.com", null).path()).isEqualTo("/");
        assertThat(new Route("web.example.com", "  ").path()).isEqualTo("/");
    }

    @Test
    void keepsExplicitPath() {
        assertThat(new Route("web.example.com", "/v1").path()).isEqualTo("/v1");
    }

    @Test
    void rejectsBlankHost() {
        assertThatThrownBy(() -> new Route(" ", "/")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsHostWithSlashOrSpace() {
        assertThatThrownBy(() -> new Route("web.example.com/x", "/")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Route("web example", "/")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsRelativePath() {
        assertThatThrownBy(() -> new Route("web.example.com", "v1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsPortTo80() {
        assertThat(new Route("web.example.com", "/").port()).isEqualTo(80);
    }

    @Test
    void rejectsInvalidPort() {
        assertThatThrownBy(() -> new Route("web.example.com", "/", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Route("web.example.com", "/", 70000)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encodeDecodeRoundTrip() {
        java.util.List<Route> routes = java.util.List.of(
                new Route("web.example.com", "/", 80),
                new Route("api.example.com", "/v1", 8080));

        assertThat(Route.decode(Route.encode(routes)))
                .containsExactlyInAnyOrderElementsOf(routes);
    }
}
