package io.d4y.api;

import io.d4y.adapter.git.GitConfigSync;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.ConfigVersion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * @param mode   {@code git} oder {@code local}
     * @param source Repo-URL (git) bzw. lokaler Pfad (local)
     */
    public record ConfigView(String mode, String source, String branch, String commit,
                             String author, String message, String time) {
    }
}
