package io.d4y.adapter.yaml;

import io.d4y.TestFixtures;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.DesiredState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class YamlDesiredStateSourceTest {

    private static D4yProperties propsFor(Path dir) {
        return TestFixtures.props(dir.toString());
    }

    @Test
    void readsApplicationsFromYamlFiles(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("web.yaml"), "name: web\nimage: nginx:1.27-alpine\n");
        Files.writeString(dir.resolve("cache.yml"), "name: cache\nimage: redis:7\n");

        DesiredState state = new YamlDesiredStateSource(propsFor(dir)).load();

        assertThat(state.applications())
                .extracting(a -> a.name() + "=" + a.image().reference())
                .containsExactlyInAnyOrder("web=nginx:1.27-alpine", "cache=redis:7");
    }

    @Test
    void readsListOfApplicationsFromSingleFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("apps.yaml"),
                "- name: a\n  image: nginx:1\n- name: b\n  image: nginx:2\n");

        DesiredState state = new YamlDesiredStateSource(propsFor(dir)).load();

        assertThat(state.applications()).hasSize(2)
                .extracting(a -> a.name()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void parsesNamedVolumes(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("web.yaml"),
                "name: web\nimage: nginx:1.27-alpine\nvolumes:\n"
                        + "  - name: html\n    path: /usr/share/nginx/html\n"
                        + "  - name: cache\n    path: /var/cache/nginx\n");

        DesiredState state = new YamlDesiredStateSource(propsFor(dir)).load();

        assertThat(state.applications()).singleElement()
                .satisfies(a -> assertThat(a.volumes())
                        .extracting(v -> v.name() + "@" + v.path())
                        .containsExactly("html@/usr/share/nginx/html", "cache@/var/cache/nginx"));
    }

    @Test
    void appWithoutVolumesHasEmptyList(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("web.yaml"), "name: web\nimage: nginx:1.27-alpine\n");

        DesiredState state = new YamlDesiredStateSource(propsFor(dir)).load();

        assertThat(state.applications()).singleElement()
                .satisfies(a -> assertThat(a.volumes()).isEmpty());
    }

    @Test
    void parsesBackupFlag(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("web.yaml"), "name: web\nimage: nginx:1.27-alpine\nbackup: true\n");
        Files.writeString(dir.resolve("cache.yml"), "name: cache\nimage: redis:7\n");

        DesiredState state = new YamlDesiredStateSource(propsFor(dir)).load();

        assertThat(state.applications())
                .filteredOn(a -> a.name().equals("web")).singleElement()
                .satisfies(a -> assertThat(a.backup()).isTrue());
        assertThat(state.applications())
                .filteredOn(a -> a.name().equals("cache")).singleElement()
                .satisfies(a -> assertThat(a.backup()).isFalse());
    }

    @Test
    void parsesEnv(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("web.yaml"),
                "name: web\nimage: nginx:1.27-alpine\nenv:\n  LOG_LEVEL: debug\n  PORT: \"8080\"\n");

        DesiredState state = new YamlDesiredStateSource(propsFor(dir)).load();

        assertThat(state.applications()).singleElement().satisfies(a ->
                assertThat(a.env()).containsEntry("LOG_LEVEL", "debug").containsEntry("PORT", "8080"));
    }

    @Test
    void parsesRoutes(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("web.yaml"),
                "name: web\nimage: nginx:1.27-alpine\nroutes:\n"
                        + "  - host: web.example.com\n"
                        + "  - host: api.example.com\n    path: /v1\n");

        DesiredState state = new YamlDesiredStateSource(propsFor(dir)).load();

        assertThat(state.applications()).singleElement()
                .satisfies(a -> assertThat(a.routes())
                        .extracting(r -> r.host() + r.path())
                        .containsExactly("web.example.com/", "api.example.com/v1"));
    }

    @Test
    void parsesPerRouteTls(@TempDir Path dir) throws IOException {
        // ADR-0028: optionales 'tls' pro Route; fehlt es, bleibt es null (globaler Default).
        Files.writeString(dir.resolve("web.yaml"),
                "name: web\nimage: nginx:1.27-alpine\nroutes:\n"
                        + "  - host: secure.example.com\n    tls: true\n"
                        + "  - host: plain.example.com\n    tls: false\n"
                        + "  - host: default.example.com\n");

        DesiredState state = new YamlDesiredStateSource(propsFor(dir)).load();

        assertThat(state.applications()).singleElement()
                .satisfies(a -> assertThat(a.routes())
                        .extracting(r -> r.host() + "=" + r.tls())
                        .containsExactly("secure.example.com=true", "plain.example.com=false",
                                "default.example.com=null"));
    }

    @Test
    void rejectsRouteWithoutHost(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("web.yaml"),
                "name: web\nimage: nginx:1.27-alpine\nroutes:\n  - path: /v1\n");

        YamlDesiredStateSource source = new YamlDesiredStateSource(propsFor(dir));

        org.assertj.core.api.Assertions.assertThatThrownBy(source::load)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsVolumeWithoutPath(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("web.yaml"),
                "name: web\nimage: nginx:1.27-alpine\nvolumes:\n  - name: html\n");

        YamlDesiredStateSource source = new YamlDesiredStateSource(propsFor(dir));

        org.assertj.core.api.Assertions.assertThatThrownBy(source::load)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingDirectoryYieldsEmptyState(@TempDir Path dir) {
        DesiredState state = new YamlDesiredStateSource(propsFor(dir.resolve("does-not-exist"))).load();

        assertThat(state.applications()).isEmpty();
    }
}
