package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.domain.model.ContainerDetails;
import io.d4y.domain.model.ExecResult;
import io.d4y.port.ContainerBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Docker-Adapter für container-nahe operative Aktionen (ADR-0005/0013): restart/stop/logs/inspect/exec
 * auf einer Container-ID über die Docker-Engine-API. Das Deployment läuft über Docker Compose
 * (ADR-0029), nicht über diesen Adapter.
 */
@Component
public class DockerContainerBackend implements ContainerBackend {

    private static final Logger log = LoggerFactory.getLogger(DockerContainerBackend.class);

    private final DockerHttpClient docker;
    private final ObjectMapper json;

    public DockerContainerBackend(DockerHttpClient docker, ObjectMapper json) {
        this.docker = docker;
        this.json = json;
    }

    @Override
    public void restart(String containerId) {
        DockerHttpClient.Response r = docker.post("/containers/" + containerId + "/restart?t=5", null);
        requireOneOf(r, "Container neu starten", 204);
        log.info("Container {} neu gestartet", shortId(containerId));
    }

    @Override
    public void stop(String containerId) {
        DockerHttpClient.Response r = docker.post("/containers/" + containerId + "/stop?t=5", null);
        requireOneOfOrMissing(r, "Container stoppen", 204, 304);
        log.info("Container {} gestoppt", shortId(containerId));
    }

    @Override
    public String logs(String containerId, int tail) {
        DockerHttpClient.ResponseBytes r =
                docker.getBytes("/containers/" + containerId + "/logs?stdout=1&stderr=1&tail=" + tail);
        if (!r.isSuccess()) {
            throw new DockerApiException("Logs lesen", r.status(), "");
        }
        return DockerStreamDemux.demux(r.body());
    }

    @Override
    public ContainerDetails inspect(String containerId) {
        DockerHttpClient.Response r = docker.get("/containers/" + containerId + "/json");
        require(r, "Container inspizieren");
        JsonNode n = readTree(r.body());
        JsonNode state = n.path("State");
        JsonNode config = n.path("Config");
        List<String> env = new ArrayList<>();
        if (config.path("Env").isArray()) {
            config.path("Env").forEach(e -> env.add(e.asText()));
        }
        String name = n.path("Name").asText("");
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return new ContainerDetails(
                n.path("Id").asText(),
                name,
                config.path("Image").asText(),
                state.path("Status").asText(),
                state.path("Status").asText(),
                n.path("Created").asText(),
                env);
    }

    @Override
    public ExecResult exec(String containerId, List<String> cmd) {
        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("AttachStdout", true);
        createBody.put("AttachStderr", true);
        createBody.put("Tty", false);
        createBody.put("Cmd", cmd);
        DockerHttpClient.Response created = docker.post("/containers/" + containerId + "/exec", toJson(createBody));
        require(created, "exec erzeugen");
        String execId = readTree(created.body()).path("Id").asText();

        Map<String, Object> startBody = new LinkedHashMap<>();
        startBody.put("Detach", false);
        startBody.put("Tty", false);
        DockerHttpClient.ResponseBytes started = docker.postBytes("/exec/" + execId + "/start", toJson(startBody));
        if (!started.isSuccess()) {
            throw new DockerApiException("exec starten", started.status(), "");
        }
        String output = DockerStreamDemux.demux(started.body());

        DockerHttpClient.Response inspectExec = docker.get("/exec/" + execId + "/json");
        int exitCode = inspectExec.isSuccess() ? readTree(inspectExec.body()).path("ExitCode").asInt(0) : -1;
        return new ExecResult(output, exitCode);
    }

    // --- Hilfsfunktionen -----------------------------------------------------------------

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

    private static boolean contains(int[] codes, int value) {
        for (int c : codes) {
            if (c == value) {
                return true;
            }
        }
        return false;
    }

    private static String shortId(String id) {
        return id == null ? "?" : id.substring(0, Math.min(12, id.length()));
    }
}
