package io.d4y.adapter.compose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.d4y.domain.model.AppProject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest die App-Sidecar {@code d4y.yaml} (ADR-0029): Routes (externer Ingress) und Backup-Opt-in.
 * Genutzt von Override-Generierung, Status und Backup. Fehlt die Sidecar, sind beide Listen leer.
 */
public final class Sidecar {

    private static final YAMLMapper YAML = new YAMLMapper();

    private Sidecar() {
    }

    /** Eine Route aus {@code d4y.yaml}: exponiert einen Compose-Service unter einem Hostnamen. */
    public record Route(String service, String host, String path, int port, Boolean tls) {
    }

    /** Routes der App; leer, wenn keine Sidecar/keine gültigen Routes vorhanden. */
    public static List<Route> routes(AppProject project) {
        List<Route> result = new ArrayList<>();
        if (!project.hasSidecar()) {
            return result;
        }
        JsonNode routes = read(project).get("routes");
        if (routes == null || !routes.isArray()) {
            return result;
        }
        for (JsonNode r : routes) {
            String service = text(r, "service");
            String host = text(r, "host");
            if (service.isBlank() || host.isBlank()) {
                continue;
            }
            String path = text(r, "path");
            int port = r.hasNonNull("port") ? r.get("port").asInt(80) : 80;
            Boolean tls = r.hasNonNull("tls") ? r.get("tls").asBoolean() : null;
            result.add(new Route(service, host, path.isBlank() ? "/" : path, port, tls));
        }
        return result;
    }

    /** Compose-Volume-Namen der App, die gesichert werden sollen ({@code backup.volumes}). */
    public static List<String> backupVolumes(AppProject project) {
        List<String> result = new ArrayList<>();
        if (!project.hasSidecar()) {
            return result;
        }
        JsonNode volumes = read(project).path("backup").get("volumes");
        if (volumes != null && volumes.isArray()) {
            volumes.forEach(v -> {
                if (!v.asText().isBlank()) {
                    result.add(v.asText());
                }
            });
        }
        return result;
    }

    private static JsonNode read(AppProject project) {
        try {
            return YAML.readTree(Files.readString(project.sidecar()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? "" : v.asText();
    }
}
