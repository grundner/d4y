package io.d4y.adapter.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.d4y.adapter.docker.DockerEdgeProxy;
import io.d4y.domain.model.AppProject;
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
 * nur d4y-Belange ergänzt. In Phase 1: jeder Service tritt dem externen {@code d4y}-Netz bei (damit
 * Traefik routen und interne Service-Discovery greifen kann), ohne vorhandene Netze des Nutzers zu
 * verlieren. Die Route→Traefik-Label-Injektion aus der Sidecar {@code d4y.yaml} folgt in Phase 2.
 *
 * <p>Das Override wird in ein separates Arbeitsverzeichnis geschrieben, nicht in die (ggf.
 * git-verwaltete) App-Quelle.
 */
@Component
public class ComposeOverrideGenerator {

    private final ObjectMapper yaml = new YAMLMapper();
    private final Path workDir;

    public ComposeOverrideGenerator(@Value("${d4y.compose.work-dir:./.d4y-compose}") String workDir) {
        this.workDir = Path.of(workDir);
    }

    /** Generiert das Override und gibt seinen Pfad zurück. */
    public Path generate(AppProject project) {
        try {
            JsonNode compose = yaml.readTree(Files.readString(project.composeFile()));
            Map<String, Object> doc = new LinkedHashMap<>();
            // Externes, von d4y gemanagtes Netz (existiert bereits; ensureNetwork legt es an).
            doc.put("networks", Map.of(DockerEdgeProxy.NETWORK, Map.of("external", true)));

            Map<String, Object> services = new LinkedHashMap<>();
            JsonNode svcNode = compose.get("services");
            if (svcNode != null && svcNode.isObject()) {
                svcNode.fieldNames().forEachRemaining(name -> {
                    List<String> nets = serviceNetworks(svcNode.get(name));
                    if (!nets.contains(DockerEdgeProxy.NETWORK)) {
                        nets.add(DockerEdgeProxy.NETWORK);
                    }
                    services.put(name, Map.of("networks", nets));
                });
            }
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
}
