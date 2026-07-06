package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.D4yLabels;
import io.d4y.domain.model.ImageRef;
import io.d4y.domain.model.ObservedContainer;
import io.d4y.port.ContainerBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Docker-Adapter des {@link ContainerBackend}-Ports (ADR-0005, erste Implementierung).
 *
 * <p>Spricht über {@link DockerHttpClient} mit der Engine-API. Nur Container mit dem
 * {@link D4yLabels#MANAGED}-Label werden berücksichtigt.
 */
@Component
public class DockerContainerBackend implements ContainerBackend {

    private static final Logger log = LoggerFactory.getLogger(DockerContainerBackend.class);
    private static final String MANAGED_FILTER =
            "{\"label\":[\"" + D4yLabels.MANAGED + "=true\"]}";

    private final DockerHttpClient docker;
    private final ObjectMapper json;

    public DockerContainerBackend(DockerHttpClient docker, ObjectMapper json) {
        this.docker = docker;
        this.json = json;
    }

    @Override
    public List<ObservedContainer> observe() {
        String path = "/containers/json?all=true&filters=" + enc(MANAGED_FILTER);
        DockerHttpClient.Response res = docker.get(path);
        require(res, "Container auflisten");
        List<ObservedContainer> result = new ArrayList<>();
        for (JsonNode node : readTree(res.body())) {
            Map<String, String> labels = labelsOf(node);
            String appName = labels.getOrDefault(D4yLabels.APP, "");
            if (appName.isBlank()) {
                continue; // ohne App-Zuordnung nicht steuerbar
            }
            String imageRef = labels.getOrDefault(D4yLabels.IMAGE, node.path("Image").asText("unknown"));
            boolean running = "running".equals(node.path("State").asText());
            result.add(new ObservedContainer(node.path("Id").asText(), appName, ImageRef.of(imageRef), running));
        }
        return result;
    }

    @Override
    public void ensureImage(ImageRef image) {
        String path = "/images/create?fromImage=" + enc(image.repository()) + "&tag=" + enc(image.tag());
        DockerHttpClient.Response res = docker.post(path, null);
        require(res, "Image beziehen: " + image);
        log.info("Image sichergestellt: {}", image);
    }

    @Override
    public String run(ContainerSpec spec) {
        ensureImage(spec.image());

        Map<String, Object> labels = new LinkedHashMap<>();
        labels.put(D4yLabels.MANAGED, "true");
        labels.put(D4yLabels.APP, spec.appName());
        labels.put(D4yLabels.IMAGE, spec.image().reference());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Image", spec.image().reference());
        body.put("Labels", labels);

        String createPath = "/containers/create?name=" + enc("d4y_" + spec.appName());
        DockerHttpClient.Response created = docker.post(createPath, toJson(body));
        require(created, "Container erzeugen: " + spec.appName());
        String id = readTree(created.body()).path("Id").asText();

        DockerHttpClient.Response started = docker.post("/containers/" + id + "/start", null);
        requireOneOf(started, "Container starten: " + spec.appName(), 204, 304);
        log.info("App '{}' gestartet (Container {})", spec.appName(), shortId(id));
        return id;
    }

    @Override
    public void stopAndRemove(String containerId) {
        DockerHttpClient.Response stopped = docker.post("/containers/" + containerId + "/stop?t=5", null);
        requireOneOfOrMissing(stopped, "Container stoppen", 204, 304);
        DockerHttpClient.Response removed = docker.delete("/containers/" + containerId + "?force=true");
        requireOneOfOrMissing(removed, "Container entfernen", 204);
        log.info("Container {} gestoppt und entfernt", shortId(containerId));
    }

    // --- Hilfsfunktionen -----------------------------------------------------------------

    private Map<String, String> labelsOf(JsonNode node) {
        Map<String, String> labels = new LinkedHashMap<>();
        JsonNode labelNode = node.get("Labels");
        if (labelNode != null && labelNode.isObject()) {
            labelNode.fields().forEachRemaining(e -> labels.put(e.getKey(), e.getValue().asText()));
        }
        return labels;
    }

    private JsonNode readTree(String body) {
        try {
            return json.readTree(body.isBlank() ? "{}" : body);
        } catch (IOException e) {
            throw new UncheckedIOException("Docker-Antwort nicht parsebar: " + body, e);
        }
    }

    private String toJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException("JSON-Serialisierung fehlgeschlagen", e);
        }
    }

    private void require(DockerHttpClient.Response res, String what) {
        if (!res.isSuccess()) {
            throw new DockerApiException(what, res.status(), res.body());
        }
    }

    private void requireOneOf(DockerHttpClient.Response res, String what, int... okCodes) {
        if (!contains(okCodes, res.status())) {
            throw new DockerApiException(what, res.status(), res.body());
        }
    }

    /** Wie {@link #requireOneOf}, toleriert aber zusätzlich 404 (Ressource bereits weg). */
    private void requireOneOfOrMissing(DockerHttpClient.Response res, String what, int... okCodes) {
        if (res.status() == 404 || contains(okCodes, res.status())) {
            return;
        }
        throw new DockerApiException(what, res.status(), res.body());
    }

    private static boolean contains(int[] codes, int code) {
        for (int c : codes) {
            if (c == code) {
                return true;
            }
        }
        return false;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String shortId(String id) {
        return id.length() > 12 ? id.substring(0, 12) : id;
    }
}
