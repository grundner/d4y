package io.d4y.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Deklarierter externer Ingress einer {@link Application}: Zuordnung eines Hostnamens (und ggf.
 * Pfads) auf einen Ziel-Port der App. → {@code docs/domain/route.md}, ADR-0010, ADR-0016, ADR-0028.
 *
 * @param host Hostname, unter dem die App erreichbar sein soll (z. B. {@code web.example.com})
 * @param path Pfad-Präfix; Default {@code /}
 * @param port Ziel-Port im Container, an den der Reverse Proxy weiterreicht; Default {@code 80}
 * @param tls TLS für diese Route (ADR-0028): {@code true} = HTTPS ({@code websecure}),
 *            {@code false} = reines HTTP ({@code web}), {@code null} = globaler Default
 */
public record Route(String host, String path, int port, Boolean tls) {

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
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Route-Port außerhalb 1..65535: " + port);
        }
    }

    /** Bequemer Konstruktor mit Default-Port 80, TLS gemäß globalem Default. */
    public Route(String host, String path) {
        this(host, path, 80, null);
    }

    /** Bequemer Konstruktor mit TLS gemäß globalem Default. */
    public Route(String host, String path, int port) {
        this(host, path, port, null);
    }

    /** Effektives TLS dieser Route: explizite Angabe, sonst der übergebene globale Default. */
    public boolean tlsEnabled(boolean defaultEnabled) {
        return tls != null ? tls : defaultEnabled;
    }

    /**
     * Deterministische Kodierung einer Route-Liste für das {@code d4y.routes}-Label und den
     * Drift-Vergleich: sortiert, je Eintrag {@code host|path|port|tls}, getrennt durch {@code ;}.
     * {@code tls} ist {@code ""} (Default), {@code "true"} oder {@code "false"}.
     */
    public static String encode(List<Route> routes) {
        return routes.stream()
                .sorted(Comparator.comparing(Route::host).thenComparing(Route::path).thenComparingInt(Route::port))
                .map(r -> r.host() + "|" + r.path() + "|" + r.port() + "|" + (r.tls() == null ? "" : r.tls()))
                .collect(Collectors.joining(";"));
    }

    /**
     * Umkehrung von {@link #encode(List)}; leere/nullwertige Eingabe ergibt eine leere Liste.
     * Abwärtstolerant: Einträge im alten Format {@code host|path|port} (ohne TLS) werden akzeptiert.
     */
    public static List<Route> decode(String encoded) {
        List<Route> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }
        for (String part : encoded.split(";")) {
            String[] f = part.split("\\|", 4);
            if (f.length >= 3) {
                Boolean tls = (f.length >= 4 && !f[3].isBlank()) ? Boolean.valueOf(f[3]) : null;
                result.add(new Route(f[0], f[1], Integer.parseInt(f[2]), tls));
            }
        }
        return result;
    }
}
