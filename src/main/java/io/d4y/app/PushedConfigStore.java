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
import java.util.Comparator;
import java.util.Map;

/**
 * Persistiert den per Push gelieferten Sollzustand (ADR-0025/0029) als <b>Verzeichnisbaum</b> im
 * Desired-Verzeichnis ({@code d4y.desired-state.path}): je App ein Verzeichnis mit {@code compose.yaml}
 * und Begleitdateien. Die Schlüssel der Nutzlast sind <b>relative Pfade</b> (z. B.
 * {@code web/compose.yaml}, {@code web/d4y.yaml}, {@code web/Dockerfile}).
 *
 * <p>{@link io.d4y.adapter.compose.FileSystemAppProjectSource} liest von dort; der Reconcile-Loop heilt
 * daraus auch ohne weiteren Push und über Neustarts hinweg. Voller Replace: der Inhalt des
 * Desired-Verzeichnisses wird ersetzt.
 */
@Component
public class PushedConfigStore {

    private static final Logger log = LoggerFactory.getLogger(PushedConfigStore.class);

    private final Path dir;

    public PushedConfigStore(D4yProperties properties) {
        this.dir = Path.of(properties.desiredState().path());
    }

    /**
     * Ersetzt den gesamten gepushten Sollzustand (relativer Pfad → Inhalt): validiert alle Pfade,
     * leert das Desired-Verzeichnis und schreibt den gelieferten Baum.
     *
     * @throws IllegalArgumentException bei absolutem/traversierendem Pfad (vor jedem Schreiben)
     */
    public synchronized void replaceAll(Map<String, String> files) {
        if (files == null) {
            return;
        }
        // Alle Pfade zuerst validieren — bevor irgendetwas geschrieben wird.
        for (String path : files.keySet()) {
            if (!isSafeRelative(path)) {
                throw new IllegalArgumentException("Ungültiger Config-Pfad: " + path);
            }
        }
        try {
            Files.createDirectories(dir);
            clearContents(dir); // voller Replace des Desired-Baums
            for (Map.Entry<String, String> e : files.entrySet()) {
                Path target = dir.resolve(e.getKey()).normalize();
                Files.createDirectories(target.getParent());
                Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
                Files.write(tmp, e.getValue().getBytes(StandardCharsets.UTF_8));
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            log.info("Sollzustand per Push aktualisiert ({} Datei(en))", files.size());
        } catch (IOException ex) {
            throw new UncheckedIOException("Gepushter Sollzustand konnte nicht geschrieben werden", ex);
        }
    }

    /**
     * Relativer Pfad ohne Traversal: nicht leer, nicht absolut, keine {@code .}/{@code ..}-Segmente,
     * kein Backslash oder Null-Zeichen. Slashes (Unterverzeichnisse) sind erlaubt.
     */
    private static boolean isSafeRelative(String path) {
        if (path == null || path.isBlank() || path.startsWith("/") || path.contains("\\") || path.contains("\0")) {
            return false;
        }
        for (String seg : path.split("/")) {
            if (seg.isEmpty() || ".".equals(seg) || "..".equals(seg)) {
                return false;
            }
        }
        return true;
    }

    /** Löscht rekursiv den Inhalt des Verzeichnisses (das Verzeichnis selbst bleibt). */
    private static void clearContents(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.filter(p -> !p.equals(root))
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }
}
