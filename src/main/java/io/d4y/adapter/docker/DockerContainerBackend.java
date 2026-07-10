package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.domain.model.ContainerDetails;
import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.D4yLabels;
import io.d4y.domain.model.ExecResult;
import io.d4y.domain.model.ImageRef;
import io.d4y.domain.model.ObservedContainer;
import io.d4y.domain.model.Route;
import io.d4y.domain.model.VolumeMapping;
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
    private final DockerEdgeProxy edgeProxy;
    private final io.d4y.port.VolumeBackup volumeBackup;

    public DockerContainerBackend(DockerHttpClient docker, ObjectMapper json, DockerEdgeProxy edgeProxy,
                                  io.d4y.port.VolumeBackup volumeBackup) {
        this.docker = docker;
        this.json = json;
        this.edgeProxy = edgeProxy;
        this.volumeBackup = volumeBackup;
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
            List<VolumeMapping> volumes = VolumeMapping.decode(labels.getOrDefault(D4yLabels.VOLUMES, ""));
            List<Route> routes = Route.decode(labels.getOrDefault(D4yLabels.ROUTES, ""));
            Map<String, String> env = decodeEnv(labels.getOrDefault(D4yLabels.ENV, ""));
            result.add(new ObservedContainer(node.path("Id").asText(), appName, ImageRef.of(imageRef),
                    running, volumes, routes, env));
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

    /**
     * Stellt ein von D4Y verwaltetes Named Volume sicher.
     *
     * @return {@code true}, wenn das Volume <b>neu</b> angelegt wurde (vorher nicht existent)
     */
    private boolean ensureVolume(String volName, String appName) {
        if (volumeExists(volName)) {
            return false;
        }
        Map<String, Object> labels = new LinkedHashMap<>();
        labels.put(D4yLabels.MANAGED, "true");
        labels.put(D4yLabels.APP, appName);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Name", volName);
        body.put("Labels", labels);
        DockerHttpClient.Response res = docker.post("/volumes/create", toJson(body));
        require(res, "Volume sicherstellen: " + volName);
        return true;
    }

    private boolean volumeExists(String volName) {
        return docker.get("/volumes/" + enc(volName)).status() == 200;
    }

    /**
     * Übersetzt die deklarierten {@link Route}s in Traefik-Router/Service-Labels (Docker-Provider,
     * ADR-0016). Je Route entstehen ein Router (Host/PathPrefix) und ein Service (Ziel-Port).
     */
    private void addTraefikLabels(Map<String, Object> labels, ContainerSpec spec) {
        if (spec.routes().isEmpty()) {
            return;
        }
        labels.put("traefik.enable", "true");
        String entrypoints = edgeProxy.routerEntrypoints();
        String certResolver = edgeProxy.certResolver();
        String base = sanitize(spec.appName());
        int i = 0;
        for (Route r : spec.routes()) {
            String rn = "d4y-" + base + "-" + i;
            String router = "traefik.http.routers." + rn + ".";
            String rule = "Host(`" + r.host() + "`)";
            if (!"/".equals(r.path())) {
                rule += " && PathPrefix(`" + r.path() + "`)";
            }
            labels.put(router + "rule", rule);
            labels.put(router + "entrypoints", entrypoints);
            labels.put(router + "service", rn);
            labels.put(router + "tls", "true");
            if (certResolver != null) {
                labels.put(router + "tls.certresolver", certResolver);
            }
            labels.put("traefik.http.services." + rn + ".loadbalancer.server.port", String.valueOf(r.port()));
            i++;
        }
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9-]", "-");
    }

    /** Dekodiert das {@code d4y.env}-Label (JSON-Objekt) zurück in eine Map. */
    private Map<String, String> decodeEnv(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        readTree(encoded).fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText()));
        return result;
    }

    @Override
    public String run(ContainerSpec spec) {
        ensureImage(spec.image());

        Map<String, Object> labels = new LinkedHashMap<>();
        labels.put(D4yLabels.MANAGED, "true");
        labels.put(D4yLabels.APP, spec.appName());
        labels.put(D4yLabels.IMAGE, spec.image().reference());
        labels.put(D4yLabels.VOLUMES, VolumeMapping.encode(spec.volumes()));
        labels.put(D4yLabels.ROUTES, Route.encode(spec.routes()));
        labels.put(D4yLabels.ENV, toJson(spec.env()));
        addTraefikLabels(labels, spec);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Image", spec.image().reference());
        body.put("Labels", labels);
        if (!spec.env().isEmpty()) {
            body.put("Env", spec.env().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList());
        }
        if (!spec.volumes().isEmpty()) {
            List<Map<String, Object>> mounts = new ArrayList<>();
            for (VolumeMapping v : spec.volumes()) {
                String volName = "d4y_" + spec.appName() + "_" + v.name();
                boolean created = ensureVolume(volName, spec.appName());
                // Restore nur bei neuem/leerem Volume und aktivem Backup (ADR-0020) — vor App-Start.
                if (created && spec.backup() && volumeBackup.enabled()) {
                    volumeBackup.restore(spec.appName(), v.name());
                }
                Map<String, Object> mount = new LinkedHashMap<>();
                mount.put("Type", "volume");
                mount.put("Source", volName);
                mount.put("Target", v.path());
                mounts.add(mount);
            }
            Map<String, Object> hostConfig = new LinkedHashMap<>();
            hostConfig.put("Mounts", mounts);
            body.put("HostConfig", hostConfig);
        }

        // Alle verwalteten Container hängen am gemeinsamen d4y-Netz (Ingress via Traefik +
        // Grundlage interne Service-Discovery); Alias = App-Name. ADR-0016.
        edgeProxy.ensureNetwork();
        Map<String, Object> endpoint = new LinkedHashMap<>();
        endpoint.put("Aliases", edgeProxy.networkAliases(spec.appName()));
        body.put("NetworkingConfig", Map.of("EndpointsConfig", Map.of(DockerEdgeProxy.NETWORK, endpoint)));

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
