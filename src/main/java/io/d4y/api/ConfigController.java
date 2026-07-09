package io.d4y.api;

import io.d4y.adapter.git.GitConfigSync;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.ConfigVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Liefert den aktuellen Stand der Sollzustands-Quelle (Config-Repository, ADR-0019). Read-only;
 * enthält niemals Zugangsdaten.
 */
@RestController
@RequestMapping("/api")
public class ConfigController {

    private final GitConfigSync git;
    private final String localPath;

    public ConfigController(GitConfigSync git, D4yProperties properties) {
        this.git = git;
        this.localPath = properties.desiredState().path();
    }

    @GetMapping("/config")
    public ConfigView config() {
        if (!git.enabled()) {
            return new ConfigView("local", localPath, null, null, null, null, null);
        }
        ConfigVersion v = git.info().orElse(null);
        return new ConfigView("git", git.url(),
                v == null ? null : v.branch(),
                v == null ? null : v.commit(),
                v == null ? null : v.author(),
                v == null ? null : v.message(),
                v == null ? null : v.time());
    }

    /** Liste der deklarierten YAML-Dateien (relativ) aus der aktuellen Sollzustands-Quelle. */
    @GetMapping("/config/files")
    public FilesView files() {
        Path dir = git.enabled() ? git.desiredDir() : Path.of(localPath);
        List<String> files = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> !p.toString().contains(java.io.File.separator + ".git" + java.io.File.separator))
                        .filter(ConfigController::isYaml)
                        .map(p -> dir.relativize(p).toString())
                        .sorted()
                        .forEach(files::add);
            } catch (IOException e) {
                // nicht lesbar → leere Liste
            }
        }
        return new FilesView(files);
    }

    private static boolean isYaml(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".yaml") || n.endsWith(".yml");
    }

    /**
     * @param mode   {@code git} oder {@code local}
     * @param source Repo-URL (git) bzw. lokaler Pfad (local)
     */
    public record ConfigView(String mode, String source, String branch, String commit,
                             String author, String message, String time) {
    }

    public record FilesView(List<String> files) {
    }
}
