package io.d4y.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deklaration eines von der Engine verwalteten <b>Named Volumes</b> (Standard-Mount).
 *
 * <p>Ein Volume deklariert nur seinen Namen und den {@code path} (Mount-Pfad im Container) —
 * der <b>Inhalt</b> ist nie Teil des Soll-Zustands. Bind Mounts sind hier bewusst nicht
 * abgebildet (nur für Host-Integration vorgesehen). → {@code docs/domain/volume.md}, ADR-0009
 */
public record VolumeMapping(String name, String path) {

    /** Gültiges Docker-Volume-Namenssegment. */
    private static final Pattern NAME = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_.-]*");

    public VolumeMapping {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(path, "path");
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Ungültiger Volume-Name: '" + name + "'");
        }
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Volume-Pfad muss absolut sein: '" + path + "'");
        }
    }

    /**
     * Deterministische Kodierung einer Volume-Liste für das {@code d4y.volumes}-Label und den
     * Drift-Vergleich: nach Namen sortiert, je Eintrag {@code name=path}, getrennt durch {@code ;}.
     */
    public static String encode(List<VolumeMapping> volumes) {
        return volumes.stream()
                .sorted(Comparator.comparing(VolumeMapping::name))
                .map(v -> v.name() + "=" + v.path())
                .collect(Collectors.joining(";"));
    }

    /** Umkehrung von {@link #encode(List)}; leere/nullwertige Eingabe ergibt eine leere Liste. */
    public static List<VolumeMapping> decode(String encoded) {
        List<VolumeMapping> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }
        for (String part : encoded.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            result.add(new VolumeMapping(part.substring(0, eq), part.substring(eq + 1)));
        }
        return result;
    }
}
