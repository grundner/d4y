package io.d4y.adapter.git;

import io.d4y.config.D4yProperties;
import io.d4y.domain.model.ConfigVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

/**
 * Hält das Git-Config-Repository lokal als read-only Spiegel (ADR-0019): initialer Clone beim Start,
 * periodisch {@code fetch} + {@code reset --hard origin/<branch>}. Ist keine {@code url} gesetzt,
 * ist die Komponente inert und D4Y liest den lokalen {@code desired/}-Fallback.
 */
@Component
public class GitConfigSync {

    private static final Logger log = LoggerFactory.getLogger(GitConfigSync.class);

    private final D4yProperties.ConfigRepo cfg;
    private final Path localDir;

    public GitConfigSync(D4yProperties properties) {
        this.cfg = properties.configRepo();
        this.localDir = Path.of(cfg.localDir());
    }

    public boolean enabled() {
        return cfg.enabled();
    }

    public String url() {
        return cfg.url();
    }

    /** Verzeichnis mit den YAML-Dateien innerhalb des Klons ({@code path} = Unterordner, sonst Wurzel). */
    public Path desiredDir() {
        return cfg.path().isBlank() ? localDir : localDir.resolve(cfg.path());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (enabled()) {
            sync();
        }
    }

    @Scheduled(fixedDelayString = "${d4y.config-repo.poll-interval-ms:30000}")
    public void periodic() {
        if (enabled()) {
            sync();
        }
    }

    /** Klont bzw. aktualisiert den Klon idempotent. Fehler werden geloggt, nicht geworfen. */
    public synchronized void sync() {
        try {
            File dir = localDir.toFile();
            CredentialsProvider cp = credentials();
            if (new File(dir, ".git").isDirectory()) {
                try (Git git = Git.open(dir)) {
                    git.fetch().setCredentialsProvider(cp).call();
                    git.reset().setMode(ResetCommand.ResetType.HARD)
                            .setRef("origin/" + cfg.branch()).call();
                }
            } else {
                deleteRecursively(localDir); // stale/nicht-git-Verzeichnis
                Git.cloneRepository()
                        .setURI(cfg.url())
                        .setBranch(cfg.branch())
                        .setDirectory(dir)
                        .setCredentialsProvider(cp)
                        .call().close();
            }
            log.info("Config-Repo synchronisiert ({} @ {})", cfg.branch(),
                    info().map(ConfigVersion::commit).orElse("?"));
        } catch (Exception e) {
            log.warn("Config-Repo-Sync fehlgeschlagen (erneuter Versuch beim nächsten Intervall): {}",
                    e.getMessage());
            log.debug("Details", e);
        }
    }

    /** Aktueller Commit-Stand des Klons, sofern vorhanden. */
    public Optional<ConfigVersion> info() {
        File dir = localDir.toFile();
        if (!enabled() || !new File(dir, ".git").isDirectory()) {
            return Optional.empty();
        }
        try (Git git = Git.open(dir)) {
            Repository repo = git.getRepository();
            ObjectId head = repo.resolve("HEAD");
            if (head == null) {
                return Optional.empty();
            }
            try (RevWalk walk = new RevWalk(repo)) {
                RevCommit c = walk.parseCommit(head);
                String time = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(c.getCommitTime()));
                return Optional.of(new ConfigVersion(
                        head.abbreviate(7).name(),
                        cfg.branch(),
                        c.getAuthorIdent().getName(),
                        c.getShortMessage(),
                        time));
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private CredentialsProvider credentials() {
        if (cfg.token() != null && !cfg.token().isBlank()) {
            String user = cfg.username().isBlank() ? "token" : cfg.username();
            return new UsernamePasswordCredentialsProvider(user, cfg.token());
        }
        return null; // anonym (öffentliche/file://-Repos)
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }
    }
}
