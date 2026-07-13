package io.d4y.adapter.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.adapter.docker.DockerHttpClient;
import io.d4y.domain.model.ComposeProject;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Beobachtet den Ist-Zustand als Compose-Projekte (ADR-0029): listet Container mit dem Compose-
 * Projekt-Label über die Docker-Engine-API und gruppiert sie je Projekt. Read-only.
 */
@Component
public class ComposeObserver {

    private static final String PROJECT_LABEL = "com.docker.compose.project";
    private static final String SERVICE_LABEL = "com.docker.compose.service";

    private final DockerHttpClient docker;
    private final ObjectMapper json;

    public ComposeObserver(DockerHttpClient docker, ObjectMapper json) {
        this.docker = docker;
        this.json = json;
    }

    /** Alle Compose-Projekte, deren Name mit {@code prefix} beginnt (die von d4y verwalteten). */
    public List<ComposeProject> observe(String prefix) {
        DockerHttpClient.Response res = docker.get(
                "/containers/json?all=true&filters=" + enc("{\"label\":[\"" + PROJECT_LABEL + "\"]}"));
        if (!res.isSuccess()) {
            return List.of();
        }
        // Projektname → (Container-Liste), Reihenfolge stabil.
        Map<String, List<ComposeProject.Container>> byProject = new LinkedHashMap<>();
        for (JsonNode c : readTree(res.body())) {
            JsonNode labels = c.path("Labels");
            String project = labels.path(PROJECT_LABEL).asText("");
            if (project.isBlank() || !project.startsWith(prefix)) {
                continue;
            }
            byProject.computeIfAbsent(project, k -> new ArrayList<>()).add(new ComposeProject.Container(
                    c.path("Id").asText(""),
                    labels.path(SERVICE_LABEL).asText(""),
                    c.path("Image").asText(""),
                    c.path("State").asText(""),
                    c.path("Status").asText("")));
        }
        List<ComposeProject> result = new ArrayList<>();
        byProject.forEach((name, containers) -> result.add(new ComposeProject(name, containers)));
        return result;
    }

    private JsonNode readTree(String body) {
        try {
            return json.readTree(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
