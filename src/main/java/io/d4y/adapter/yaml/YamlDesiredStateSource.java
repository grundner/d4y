package io.d4y.adapter.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.Application;
import io.d4y.domain.model.DesiredState;
import io.d4y.domain.model.ImageRef;
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
        return new Application(name.asText(), ImageRef.of(image.asText()));
    }

    private static boolean isYaml(Path path) {
        String n = path.getFileName().toString().toLowerCase();
        return Files.isRegularFile(path) && (n.endsWith(".yaml") || n.endsWith(".yml"));
    }
}
