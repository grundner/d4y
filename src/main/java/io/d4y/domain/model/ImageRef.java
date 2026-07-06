package io.d4y.domain.model;

import java.util.Objects;

/**
 * Referenz auf ein unveränderliches Container-Image (z. B. {@code nginx:1.27-alpine}).
 *
 * <p>Images werden ausschließlich bezogen, niemals auf dem Zielsystem gebaut (ADR-0002).
 */
public record ImageRef(String reference) {

    public ImageRef {
        Objects.requireNonNull(reference, "reference");
        if (reference.isBlank()) {
            throw new IllegalArgumentException("Image-Referenz darf nicht leer sein");
        }
    }

    public static ImageRef of(String reference) {
        return new ImageRef(reference);
    }

    /** Repository-Anteil ohne Tag/Digest, z. B. {@code nginx} aus {@code nginx:1.27}. */
    public String repository() {
        int at = reference.indexOf('@');
        String withoutDigest = at >= 0 ? reference.substring(0, at) : reference;
        int lastColon = withoutDigest.lastIndexOf(':');
        int lastSlash = withoutDigest.lastIndexOf('/');
        // Ein ':' nach dem letzten '/' trennt den Tag; ein ':' davor gehört zum Registry-Port.
        return (lastColon > lastSlash) ? withoutDigest.substring(0, lastColon) : withoutDigest;
    }

    /** Tag-Anteil, z. B. {@code 1.27} aus {@code nginx:1.27}; {@code latest}, falls keiner. */
    public String tag() {
        if (reference.contains("@")) {
            return ""; // Digest-Referenz hat keinen Tag.
        }
        int lastColon = reference.lastIndexOf(':');
        int lastSlash = reference.lastIndexOf('/');
        return (lastColon > lastSlash) ? reference.substring(lastColon + 1) : "latest";
    }

    @Override
    public String toString() {
        return reference;
    }
}
