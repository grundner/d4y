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
    void writesDeliveredFilesToDesiredDir(@TempDir Path dir) throws Exception {
        store(dir).replaceAll(Map.of("web.yaml", "name: web\nimage: nginx"));
        assertThat(Files.readString(dir.resolve("web.yaml"))).contains("name: web");
    }

    @Test
    void removesFilesNoLongerDelivered(@TempDir Path dir) {
        PushedConfigStore s = store(dir);
        s.replaceAll(Map.of("a.yaml", "name: a\nimage: x", "b.yml", "name: b\nimage: y"));
        assertThat(dir.resolve("a.yaml")).exists();
        assertThat(dir.resolve("b.yml")).exists();

        s.replaceAll(Map.of("a.yaml", "name: a\nimage: z")); // b nicht mehr geliefert
        assertThat(dir.resolve("a.yaml")).exists();
        assertThat(dir.resolve("b.yml")).doesNotExist();
    }

    @Test
    void rejectsPathTraversalAndForeignNames(@TempDir Path dir) {
        PushedConfigStore s = store(dir);
        assertThatThrownBy(() -> s.replaceAll(Map.of("../evil.yaml", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.replaceAll(Map.of("sub/evil.yaml", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.replaceAll(Map.of("evil.txt", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        // Nichts wurde geschrieben — Validierung greift vor jedem Schreibzugriff.
        assertThat(dir.resolve("evil.yaml")).doesNotExist();
    }
}
