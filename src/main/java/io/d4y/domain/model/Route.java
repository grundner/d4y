package io.d4y.domain.model;

import java.util.Objects;

/**
 * Deklarierter externer Ingress einer {@link Application}: Zuordnung eines Hostnamens (und ggf.
 * Pfads) auf die App. → {@code docs/domain/route.md}, ADR-0010.
 *
 * <p>Erster Schnitt: rein deklarativ und sichtbar. Das Ableiten einer Reverse-Proxy-Konfiguration
 * ist eine spätere Ausbaustufe (eigene ADR).
 *
 * @param host Hostname, unter dem die App erreichbar sein soll (z. B. {@code web.example.com})
 * @param path Pfad-Präfix im Container-Routing; Default {@code /}
 */
public record Route(String host, String path) {

    public Route {
        Objects.requireNonNull(host, "host");
        if (host.isBlank()) {
            throw new IllegalArgumentException("Route-Host darf nicht leer sein");
        }
        if (host.contains("/") || host.contains(" ")) {
            throw new IllegalArgumentException("Ungültiger Route-Host: '" + host + "'");
        }
        path = (path == null || path.isBlank()) ? "/" : path;
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Route-Pfad muss mit '/' beginnen: '" + path + "'");
        }
    }
}
