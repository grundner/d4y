package io.d4y.app;

import io.d4y.config.D4yProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Persistiert den per Push gelieferten Sollzustand (ADR-0025) als YAML-Dateien im Desired-Verzeichnis
 * ({@code d4y.desired-state.path}). Der bestehende {@link io.d4y.adapter.yaml.YamlDesiredStateSource}
 * liest von dort; der Reconcile-Loop heilt daraus auch ohne weiteren Push und über Neustarts hinweg.
 */
@Component
public class PushedConfigStore {

    private static final Logger log = LoggerFactory.getLogger(PushedConfigStore.class);
    /** Sicherheit: nur einfache Dateinamen — keine Slashes/Backslashes/Pfad-Traversal. */
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9._-]+\\.(ya?ml)$");

    private final Path dir;

    public PushedConfigStore(D4yProperties properties) {
        this.dir = Path.of(properties.desiredState().path());
    }

    /**
     * Ersetzt den gesamten gepushten Sollzustand (Dateiname → YAML-Inhalt): schreibt jede Datei atomar
     * und entfernt zuvor gepushte, nun nicht mehr gelieferte YAML-Dateien.
     *
     * @throws IllegalArgumentException bei ungültigem/traversierendem Dateinamen (vor jedem Schreiben)
     */
    public synchronized void replaceAll(Map<String, String> files) {
        if (files == null) {
            return;
        }
        // Alle Namen zuerst validieren — bevor irgendetwas geschrieben wird.
        for (String name : files.keySet()) {
            if (name == null || !SAFE_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Ungültiger Config-Dateiname: " + name);
            }
        }
        try {
            Files.createDirectories(dir);
            Set<String> keep = new HashSet<>(files.keySet());
            try (Stream<Path> existing = Files.list(dir)) {
                existing.filter(PushedConfigStore::isYaml)
                        .filter(p -> !keep.contains(p.getFileName().toString()))
                        .forEach(PushedConfigStore::deleteQuietly);
            }
            for (Map.Entry<String, String> e : files.entrySet()) {
                Path target = dir.resolve(e.getKey());
                Path tmp = dir.resolve(e.getKey() + ".tmp");
                Files.write(tmp, e.getValue().getBytes(StandardCharsets.UTF_8));
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            log.info("Sollzustand per Push aktualisiert ({} Datei(en))", files.size());
        } catch (IOException ex) {
            throw new UncheckedIOException("Gepushter Sollzustand konnte nicht geschrieben werden", ex);
        }
    }

    private static boolean isYaml(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return Files.isRegularFile(p) && (n.endsWith(".yaml") || n.endsWith(".yml"));
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // best effort — der nächste Push/Reconcile korrigiert.
        }
    }
}
