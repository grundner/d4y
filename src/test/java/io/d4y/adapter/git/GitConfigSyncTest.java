package io.d4y.adapter.git;

import io.d4y.config.D4yProperties;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Klont ein lokales Test-Repo (file://) via JGit und prüft Inhalt + Commit-Info. */
class GitConfigSyncTest {

    @TempDir
    Path tmp;

    @Test
    void clonesRepoAndReadsCommitInfo() throws Exception {
        Path src = Files.createDirectories(tmp.resolve("src"));
        try (Git git = Git.init().setInitialBranch("main").setDirectory(src.toFile()).call()) {
            Files.writeString(src.resolve("nginx.yaml"), "name: nginx\nimage: nginx:1.27-alpine\n");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("init config").setSign(false)
                    .setAuthor("tester", "t@example.com")
                    .setCommitter("tester", "t@example.com").call();
        }

        Path clone = tmp.resolve("clone");
        GitConfigSync sync = new GitConfigSync(props(src.toUri().toString(), clone.toString()));

        assertThat(sync.enabled()).isTrue();
        sync.sync();

        assertThat(sync.desiredDir().resolve("nginx.yaml")).exists();
        assertThat(sync.info()).isPresent().get().satisfies(v -> {
            assertThat(v.branch()).isEqualTo("main");
            assertThat(v.author()).isEqualTo("tester");
            assertThat(v.message()).isEqualTo("init config");
            assertThat(v.commit()).hasSize(7);
        });
    }

    @Test
    void disabledWithoutUrl() {
        GitConfigSync sync = new GitConfigSync(props("", tmp.resolve("clone").toString()));

        assertThat(sync.enabled()).isFalse();
        assertThat(sync.info()).isEmpty();
    }

    private static D4yProperties props(String url, String localDir) {
        return new D4yProperties(
                new D4yProperties.Docker("/var/run/docker.sock", ""),
                new D4yProperties.DesiredState("./desired"),
                new D4yProperties.Reconcile(15000),
                new D4yProperties.Operations(
                        new D4yProperties.HoldConfig(900, 3600),
                        new D4yProperties.LogsConfig(200)),
                new D4yProperties.Ingress(true, "d4y.internal", "extern",
                        new D4yProperties.Tls(new D4yProperties.Acme("", "http", "", "", Map.of()))),
                new D4yProperties.ConfigRepo(url, "main", "", localDir, 30000, "", ""));
    }
}
