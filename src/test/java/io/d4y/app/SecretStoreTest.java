package io.d4y.app;

import io.d4y.TestFixtures;
import io.d4y.config.D4yProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretStoreTest {

    private static SecretStore store(Path file, String key) {
        return new SecretStore(TestFixtures.props("./desired",
                new D4yProperties.Secrets(key, file.toString())));
    }

    @Test
    void persistsEncryptedAndReloadsAfterRestart(@TempDir Path dir) throws Exception {
        Path file = dir.resolve(".d4y-secrets");
        store(file, "master-key").replaceAll(Map.of("GHCR_TOKEN", "s3cr3t-value"));

        assertThat(file).exists();
        // Ablage ist verschlüsselt: der Klartext-Wert taucht nicht in der Datei auf.
        String onDisk = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        assertThat(onDisk).doesNotContain("s3cr3t-value").doesNotContain("GHCR_TOKEN");

        // „Neustart": frische Instanz lädt die verschlüsselte Datei — ohne erneuten Push.
        SecretStore restarted = store(file, "master-key");
        restarted.load();
        assertThat(restarted.get("GHCR_TOKEN")).contains("s3cr3t-value");
    }

    @Test
    void resolvesPlaceholders(@TempDir Path dir) {
        SecretStore s = store(dir.resolve(".d4y-secrets"), "k");
        s.replaceAll(Map.of("TOKEN", "abc"));
        assertThat(s.resolve("Bearer ${secret:TOKEN}")).isEqualTo("Bearer abc");
        assertThat(s.resolve("kein-platzhalter")).isEqualTo("kein-platzhalter");
        assertThat(s.resolve(null)).isNull();
    }

    @Test
    void unknownPlaceholderThrows(@TempDir Path dir) {
        SecretStore s = store(dir.resolve(".d4y-secrets"), "k");
        assertThatThrownBy(() -> s.resolve("${secret:MISSING}"))
                .isInstanceOf(UnresolvedSecretException.class)
                .hasMessageContaining("MISSING");
    }

    @Test
    void ramOnlyWithoutEncryptionKey(@TempDir Path dir) {
        Path file = dir.resolve(".d4y-secrets");
        SecretStore s = store(file, ""); // kein Key ⇒ keine Persistenz
        s.replaceAll(Map.of("X", "y"));
        assertThat(file).doesNotExist();
        assertThat(s.get("X")).contains("y");
    }
}
