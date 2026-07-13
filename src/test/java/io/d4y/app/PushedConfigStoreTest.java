package io.d4y.app;

import io.d4y.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushedConfigStoreTest {

    private static PushedConfigStore store(Path dir) {
        return new PushedConfigStore(TestFixtures.props(dir.toString()));
    }

    @Test
    void writesAppDirectoryTree(@TempDir Path dir) throws Exception {
        store(dir).replaceAll(Map.of(
                "web/compose.yaml", "services:\n  web:\n    image: nginx",
                "web/d4y.yaml", "routes:\n  - service: web\n    host: web.test"));

        assertThat(Files.readString(dir.resolve("web/compose.yaml"))).contains("image: nginx");
        assertThat(Files.readString(dir.resolve("web/d4y.yaml"))).contains("web.test");
    }

    @Test
    void fullReplaceRemovesFilesNoLongerDelivered(@TempDir Path dir) {
        PushedConfigStore s = store(dir);
        s.replaceAll(Map.of("web/compose.yaml", "a", "api/compose.yaml", "b"));
        assertThat(dir.resolve("web/compose.yaml")).exists();
        assertThat(dir.resolve("api/compose.yaml")).exists();

        s.replaceAll(Map.of("web/compose.yaml", "a2")); // api nicht mehr geliefert
        assertThat(dir.resolve("web/compose.yaml")).exists();
        assertThat(dir.resolve("api")).doesNotExist();
    }

    @Test
    void allowsSubdirectoriesAndArbitraryFileTypes(@TempDir Path dir) throws Exception {
        store(dir).replaceAll(Map.of(
                "app/compose.yaml", "x",
                "app/Dockerfile", "FROM scratch",
                "app/conf/nginx.conf", "server {}"));

        assertThat(dir.resolve("app/Dockerfile")).exists();
        assertThat(Files.readString(dir.resolve("app/conf/nginx.conf"))).contains("server");
    }

    @Test
    void rejectsPathTraversalAndAbsolutePaths(@TempDir Path dir) {
        PushedConfigStore s = store(dir);
        assertThatThrownBy(() -> s.replaceAll(Map.of("../evil.yaml", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.replaceAll(Map.of("web/../../evil", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.replaceAll(Map.of("/etc/passwd", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        // Nichts wurde geschrieben — Validierung greift vor jedem Schreibzugriff.
        assertThat(dir.resolve("evil.yaml")).doesNotExist();
    }
}
