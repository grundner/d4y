package io.d4y.adapter.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Führt {@code docker compose}-Kommandos über {@link ProcessBuilder} aus (ADR-0029). Die Docker-CLI
 * verhandelt die Engine-API-Version selbst; anders als der direkte Socket-Client kann sie bauen.
 *
 * <p>Secrets werden ausschließlich als <b>Prozess-Umgebung</b> übergeben (nie auf die Kommandozeile,
 * nie geloggt).
 */
@Component
public class ComposeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ComposeExecutor.class);

    private final ObjectMapper json;
    private final String docker;
    private final long timeoutSeconds;

    public ComposeExecutor(ObjectMapper json,
                           @Value("${d4y.compose.docker-binary:}") String dockerBinary,
                           @Value("${d4y.compose.timeout-seconds:600}") long timeoutSeconds) {
        this.json = json;
        this.docker = dockerBinary.isBlank() ? resolveDocker() : dockerBinary;
        this.timeoutSeconds = timeoutSeconds;
    }

    /** {@code docker compose version} — prüft, ob das Compose-Plugin verfügbar ist. */
    public boolean available() {
        try {
            run(List.of(docker, "compose", "version"), null, Map.of(), 30);
            return true;
        } catch (RuntimeException e) {
            log.warn("`docker compose` nicht verfügbar: {}", e.getMessage());
            return false;
        }
    }

    /**
     * {@code docker compose -p <project> up -d --remove-orphans [--build]} im App-Verzeichnis.
     *
     * @param env zusätzliche Umgebungsvariablen (aufgelöste Secrets) für die Interpolation im Compose
     */
    public void up(String project, Path projectDir, Path composeFile, Path override,
                   boolean build, Map<String, String> env) {
        List<String> cmd = new ArrayList<>(List.of(
                docker, "compose",
                "--project-directory", projectDir.toString(),
                "--project-name", project,
                "-f", composeFile.toString(),
                "-f", override.toString(),
                "up", "-d", "--remove-orphans"));
        if (build) {
            cmd.add("--build");
        }
        run(cmd, projectDir, env, timeoutSeconds);
    }

    /** {@code docker compose -p <project> down} — stoppt/entfernt das Projekt (Named Volumes bleiben). */
    public void down(String project) {
        run(List.of(docker, "compose", "--project-name", project, "down", "--remove-orphans"),
                null, Map.of(), timeoutSeconds);
    }

    /** Namen aller Compose-Projekte (laufend und gestoppt), z. B. um Drift zu erkennen. */
    public List<String> listProjects() {
        String out = run(List.of(docker, "compose", "ls", "--all", "--format", "json"),
                null, Map.of(), 30);
        List<String> names = new ArrayList<>();
        try {
            JsonNode arr = json.readTree(out);
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String name = n.path("Name").asText("");
                    if (!name.isBlank()) {
                        names.add(name);
                    }
                }
            }
        } catch (IOException e) {
            throw new ComposeException("compose ls-Ausgabe nicht lesbar", e);
        }
        return names;
    }

    /** Führt ein Kommando aus, gibt stdout+stderr zurück; wirft {@link ComposeException} bei Fehler. */
    private String run(List<String> cmd, Path workingDir, Map<String, String> env, long timeout) {
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }
        pb.environment().putAll(env); // Secrets nur hier, nie im cmd
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ComposeException("Start von `docker compose` fehlgeschlagen (docker=" + docker + ")", e);
        }
        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new ComposeException("Timeout (" + timeout + "s): " + redact(cmd));
            }
        } catch (IOException e) {
            throw new ComposeException("Ausgabe von `docker compose` nicht lesbar", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ComposeException("Unterbrochen: " + redact(cmd), e);
        }
        if (process.exitValue() != 0) {
            throw new ComposeException(redact(cmd) + " → exit " + process.exitValue() + "\n" + tail(output));
        }
        return output;
    }

    /** Kommando für Logs/Fehler ohne mögliche Geheimnisse (Env steht nie im cmd). */
    private static String redact(List<String> cmd) {
        return String.join(" ", cmd);
    }

    private static String tail(String s) {
        return s.length() <= 2000 ? s : s.substring(s.length() - 2000);
    }

    /** Sucht die {@code docker}-Executable in gängigen Pfaden + PATH; Fallback: reines "docker". */
    private static String resolveDocker() {
        List<String> candidates = new ArrayList<>(List.of(
                "/usr/bin/docker", "/usr/local/bin/docker", "/opt/homebrew/bin/docker"));
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                if (!dir.isBlank()) {
                    candidates.add(dir + File.separator + "docker");
                }
            }
        }
        for (String c : candidates) {
            Path p = Path.of(c);
            if (Files.isRegularFile(p) && Files.isExecutable(p)) {
                return c;
            }
        }
        return "docker";
    }
}
