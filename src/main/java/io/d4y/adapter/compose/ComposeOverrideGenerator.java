package io.d4y.adapter.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.d4y.adapter.docker.DockerEdgeProxy;
import io.d4y.domain.model.AppProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Erzeugt je App ein <b>Override-Compose</b> (ADR-0029), das die {@code compose.yaml} sauber lässt und
 * nur d4y-Belange ergänzt:
 * <ul>
 *   <li>jeder Service tritt dem externen {@code d4y}-Netz bei (vorhandene Netze bleiben erhalten);</li>
 *   <li>je Route aus der Sidecar {@code d4y.yaml} werden am Ziel-Service die Traefik-Labels gesetzt
 *       (Router {@code Host(...)}, Entrypoint/TLS gemäß ADR-0028, Service-Port).</li>
 * </ul>
 * Das Override wird in ein separates Arbeitsverzeichnis geschrieben, nicht in die App-Quelle.
 */
@Component
public class ComposeOverrideGenerator {

    private static final Logger log = LoggerFactory.getLogger(ComposeOverrideGenerator.class);

    private final ObjectMapper yaml = new YAMLMapper();
    private final Path workDir;
    private final DockerEdgeProxy edgeProxy;

    public ComposeOverrideGenerator(@Value("${d4y.compose.work-dir:./.d4y-compose}") String workDir,
                                    DockerEdgeProxy edgeProxy) {
        this.workDir = Path.of(workDir);
        this.edgeProxy = edgeProxy;
    }

    /** Generiert das Override und gibt seinen Pfad zurück. */
    public Path generate(AppProject project) {
        try {
            JsonNode compose = yaml.readTree(Files.readString(project.composeFile()));

            Map<String, Object> services = new LinkedHashMap<>();
            JsonNode svcNode = compose.get("services");
            if (svcNode != null && svcNode.isObject()) {
                svcNode.fieldNames().forEachRemaining(name -> {
                    List<String> nets = serviceNetworks(svcNode.get(name));
                    if (!nets.contains(DockerEdgeProxy.NETWORK)) {
                        nets.add(DockerEdgeProxy.NETWORK);
                    }
                    Map<String, Object> svc = new LinkedHashMap<>();
                    svc.put("networks", nets);
                    services.put(name, svc);
                });
            }

            // Routes aus der Sidecar d4y.yaml → Traefik-Labels am Ziel-Service.
            if (project.hasSidecar()) {
                applyRoutes(project, services);
            }

            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("networks", Map.of(DockerEdgeProxy.NETWORK, Map.of("external", true)));
            doc.put("services", services);

            Path out = workDir.resolve(project.name());
            Files.createDirectories(out);
            Path file = out.resolve("d4y-override.yaml");
            Files.writeString(file, yaml.writeValueAsString(doc));
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyRoutes(AppProject project, Map<String, Object> services) throws IOException {
        JsonNode sidecar = yaml.readTree(Files.readString(project.sidecar()));
        JsonNode routes = sidecar.get("routes");
        if (routes == null || !routes.isArray()) {
            return;
        }
        boolean tlsDefault = edgeProxy.defaultTlsEnabled();
        String certResolver = edgeProxy.certResolver();
        int i = 0;
        for (JsonNode r : routes) {
            String service = text(r, "service");
            String host = text(r, "host");
            if (service.isBlank() || host.isBlank()) {
                log.warn("App '{}': Route ohne 'service'/'host' übersprungen", project.name());
                continue;
            }
            Map<String, Object> svc = (Map<String, Object>) services.get(service);
            if (svc == null) {
                log.warn("App '{}': Route verweist auf unbekannten Service '{}'", project.name(), service);
                continue;
            }
            String path = text(r, "path");
            int port = r.hasNonNull("port") ? r.get("port").asInt(80) : 80;
            boolean tls = r.hasNonNull("tls") ? r.get("tls").asBoolean() : tlsDefault;

            Map<String, Object> labels =
                    (Map<String, Object>) svc.computeIfAbsent("labels", k -> new LinkedHashMap<String, Object>());
            String rn = "d4y-" + sanitize(project.name() + "-" + service) + "-" + i;
            String router = "traefik.http.routers." + rn + ".";
            String rule = "Host(`" + host + "`)";
            if (!path.isBlank() && !"/".equals(path)) {
                rule += " && PathPrefix(`" + path + "`)";
            }
            labels.put("traefik.enable", "true");
            labels.put(router + "rule", rule);
            labels.put(router + "entrypoints", edgeProxy.entrypointForTls(tls));
            labels.put(router + "service", rn);
            if (tls) {
                labels.put(router + "tls", "true");
                if (certResolver != null) {
                    labels.put(router + "tls.certresolver", certResolver);
                }
            }
            labels.put("traefik.http.services." + rn + ".loadbalancer.server.port", String.valueOf(port));
            i++;
        }
    }

    /**
     * Bestehende Netze eines Service (Listen- oder Map-Form). Fehlen sie, gilt {@code default} — so
     * bleibt die normale Service-zu-Service-Erreichbarkeit erhalten, wenn wir {@code d4y} ergänzen.
     */
    private static List<String> serviceNetworks(JsonNode service) {
        List<String> nets = new ArrayList<>();
        JsonNode n = service == null ? null : service.get("networks");
        if (n == null || n.isNull()) {
            nets.add("default");
        } else if (n.isArray()) {
            n.forEach(e -> nets.add(e.asText()));
        } else if (n.isObject()) {
            n.fieldNames().forEachRemaining(nets::add);
        }
        return nets;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText();
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9-]", "-");
    }
}
