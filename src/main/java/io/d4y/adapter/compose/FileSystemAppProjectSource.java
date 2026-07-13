package io.d4y.adapter.compose;

import io.d4y.adapter.git.GitConfigSync;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.AppProject;
import io.d4y.port.AppProjectSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enumeriert Compose-App-Projekte (ADR-0029): jedes Unterverzeichnis des Desired-Roots, das eine
 * Compose-Datei enthält, ist eine App. Quelle ist der Git-Klon ({@link GitConfigSync}) oder — ohne
 * Git-URL — der lokale {@code d4y.desired-state.path}.
 */
@Component
public class FileSystemAppProjectSource implements AppProjectSource {

    /** Erkannte Compose-Dateinamen, in Prioritätsreihenfolge. */
    private static final List<String> COMPOSE_NAMES =
            List.of("compose.yaml", "compose.yml", "docker-compose.yaml", "docker-compose.yml");
    private static final String SIDECAR = "d4y.yaml";

    private final GitConfigSync gitSync;
    private final Path localDir;

    public FileSystemAppProjectSource(GitConfigSync gitSync, D4yProperties properties) {
        this.gitSync = gitSync;
        this.localDir = Path.of(properties.desiredState().path());
    }

    private Path root() {
        return gitSync.enabled() ? gitSync.desiredDir() : localDir;
    }

    @Override
    public List<AppProject> load() {
        Path root = root();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<AppProject> result = new ArrayList<>();
        try (var dirs = Files.list(root)) {
            List<Path> sorted = dirs.filter(Files::isDirectory).sorted(Comparator.naturalOrder()).toList();
            for (Path dir : sorted) {
                Path compose = firstComposeFile(dir);
                if (compose == null) {
                    continue; // Verzeichnis ohne Compose-Datei ⇒ keine App
                }
                Path sidecar = dir.resolve(SIDECAR);
                result.add(new AppProject(
                        dir.getFileName().toString(),
                        dir,
                        compose,
                        Files.isRegularFile(sidecar) ? sidecar : null));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private static Path firstComposeFile(Path dir) {
        for (String name : COMPOSE_NAMES) {
            Path f = dir.resolve(name);
            if (Files.isRegularFile(f)) {
                return f;
            }
        }
        return null;
    }
}
