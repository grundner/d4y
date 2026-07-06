package io.d4y.adapter.yaml;

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
        return new D4yProperties(
                new D4yProperties.Docker("/var/run/docker.sock", ""),
                new D4yProperties.DesiredState(dir.toString()),
                new D4yProperties.Reconcile(15000));
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
    void missingDirectoryYieldsEmptyState(@TempDir Path dir) {
        DesiredState state = new YamlDesiredStateSource(propsFor(dir.resolve("does-not-exist"))).load();

        assertThat(state.applications()).isEmpty();
    }
}
