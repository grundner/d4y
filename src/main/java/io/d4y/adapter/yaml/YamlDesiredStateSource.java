package io.d4y.adapter.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.Application;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.ImageRef;
import io.d4y.domain.model.Route;
import io.d4y.domain.model.VolumeMapping;
import io.d4y.port.DesiredStateSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Liest den Sollzustand aus lokalen YAML-Dateien (ADR-0011, Interim vor der Git-Anbindung).
 *
 * <p>Jede {@code *.yaml}/{@code *.yml}-Datei im konfigurierten Verzeichnis beschreibt eine
 * Application ({@code name}, {@code image}) — alternativ eine YAML-Liste solcher Objekte.
 */
@Component
public class YamlDesiredStateSource implements DesiredStateSource {

    private static final Logger log = LoggerFactory.getLogger(YamlDesiredStateSource.class);

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
    private final Path directory;

    public YamlDesiredStateSource(D4yProperties properties) {
        this.directory = Path.of(properties.desiredState().path());
    }

    @Override
    public DesiredState load() {
        if (!Files.isDirectory(directory)) {
            log.warn("Desired-State-Verzeichnis '{}' existiert nicht — leerer Sollzustand", directory);
            return DesiredState.empty();
        }
        List<Application> applications = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(YamlDesiredStateSource::isYaml)
                    .sorted()
                    .forEach(file -> readFile(file, applications));
        } catch (IOException e) {
            throw new UncheckedIOException("Desired-State-Verzeichnis nicht lesbar: " + directory, e);
        }
        return new DesiredState(applications);
    }

    private void readFile(Path file, List<Application> sink) {
        try {
            JsonNode root = yaml.readTree(Files.readAllBytes(file));
            if (root == null || root.isNull()) {
                return;
            }
            if (root.isArray()) {
                root.forEach(node -> sink.add(toApplication(node, file)));
            } else {
                sink.add(toApplication(root, file));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("YAML nicht lesbar: " + file, e);
        }
    }

    private Application toApplication(JsonNode node, Path file) {
        JsonNode name = node.get("name");
        JsonNode image = node.get("image");
        if (name == null || image == null || name.asText().isBlank() || image.asText().isBlank()) {
            throw new IllegalArgumentException("Datei " + file + ": 'name' und 'image' sind erforderlich");
        }
        return new Application(name.asText(), ImageRef.of(image.asText()),
                toVolumes(node, file), toRoutes(node, file));
    }

    private List<Route> toRoutes(JsonNode node, Path file) {
        JsonNode routes = node.get("routes");
        if (routes == null || routes.isNull()) {
            return List.of();
        }
        if (!routes.isArray()) {
            throw new IllegalArgumentException("Datei " + file + ": 'routes' muss eine Liste sein");
        }
        List<Route> result = new ArrayList<>();
        for (JsonNode r : routes) {
            JsonNode host = r.get("host");
            if (host == null || host.asText().isBlank()) {
                throw new IllegalArgumentException("Datei " + file + ": jede Route braucht 'host'");
            }
            JsonNode path = r.get("path");
            JsonNode port = r.get("port");
            result.add(new Route(host.asText(), path == null ? null : path.asText(),
                    port == null ? 80 : port.asInt(80)));
        }
        return result;
    }

    private List<VolumeMapping> toVolumes(JsonNode node, Path file) {
        JsonNode volumes = node.get("volumes");
        if (volumes == null || volumes.isNull()) {
            return List.of();
        }
        if (!volumes.isArray()) {
            throw new IllegalArgumentException("Datei " + file + ": 'volumes' muss eine Liste sein");
        }
        List<VolumeMapping> result = new ArrayList<>();
        for (JsonNode v : volumes) {
            JsonNode vName = v.get("name");
            JsonNode vPath = v.get("path");
            if (vName == null || vPath == null || vName.asText().isBlank() || vPath.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "Datei " + file + ": jedes Volume braucht 'name' und 'path'");
            }
            result.add(new VolumeMapping(vName.asText(), vPath.asText()));
        }
        return result;
    }

    private static boolean isYaml(Path path) {
        String n = path.getFileName().toString().toLowerCase();
        return Files.isRegularFile(path) && (n.endsWith(".yaml") || n.endsWith(".yml"));
    }
}
