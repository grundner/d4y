package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.D4yLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stellt die Docker-seitigen Ingress-Bausteine sicher (ADR-0016): ein gemeinsames Netzwerk
 * {@value #NETWORK} und einen verwalteten <b>Traefik</b>-Edge-Container, der Router-Konfiguration
 * direkt aus den Container-Labels (Docker-Provider) liest.
 *
 * <p>Wird beim Start und periodisch idempotent ausgeführt. Engine-spezifisch — lebt daher im
 * Docker-Adapter, nicht in der engine-neutralen Kernlogik ([ADR-0005]).
 */
@Component
public class DockerEdgeProxy {

    /** Gemeinsames Docker-Netz für Traefik und alle verwalteten App-Container. */
    public static final String NETWORK = "d4y";

    private static final String TRAEFIK_NAME = "d4y_traefik";
    private static final String TRAEFIK_IMAGE_REPO = "traefik";
    private static final String TRAEFIK_IMAGE_TAG = "v3.1";
    private static final Logger log = LoggerFactory.getLogger(DockerEdgeProxy.class);

    private static final String ACME_VOLUME = "d4y_acme";
    private static final String CERT_RESOLVER = "le";

    private final DockerHttpClient docker;
    private final ObjectMapper json;
    private final String socketPath;
    private final D4yProperties.Ingress ingress;

    public DockerEdgeProxy(DockerHttpClient docker, ObjectMapper json, D4yProperties properties) {
        this.docker = docker;
        this.json = json;
        this.socketPath = properties.docker().socketPath();
        this.ingress = properties.ingress();
    }

    /** Traefik-Entrypoints je Router: bei aktivem Redirect nur {@code websecure}, sonst beide. */
    public String routerEntrypoints() {
        return ingress.httpsRedirect() ? "websecure" : "web,websecure";
    }

    /** Cert-Resolver-Name für {@code tls.certresolver}, oder {@code null} bei self-signed. */
    public String certResolver() {
        return ingress.tls().acme().enabled() ? CERT_RESOLVER : null;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        ensure();
    }

    @Scheduled(fixedDelayString = "${d4y.reconcile.interval-ms}")
    public void periodic() {
        ensure();
    }

    /** Idempotent: Netzwerk und Edge-Proxy sicherstellen. Fehler werden geloggt, nicht geworfen. */
    public synchronized void ensure() {
        try {
            ensureNetwork();
            ensureEdgeProxy();
        } catch (RuntimeException e) {
            log.warn("Edge-Proxy/Netz konnte nicht sichergestellt werden: {}", e.getMessage());
            log.debug("Details", e);
        }
    }

    /** Legt das {@value #NETWORK}-Netz an, falls es noch nicht existiert (idempotent). */
    public void ensureNetwork() {
        DockerHttpClient.Response list = docker.get("/networks?filters=" + enc("{\"name\":[\"" + NETWORK + "\"]}"));
        if (list.isSuccess()) {
            for (JsonNode n : readTree(list.body())) {
                if (NETWORK.equals(n.path("Name").asText())) {
                    return;
                }
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Name", NETWORK);
        body.put("Driver", "bridge");
        body.put("Labels", Map.of(D4yLabels.MANAGED, "true"));
        DockerHttpClient.Response res = docker.post("/networks/create", toJson(body));
        if (res.status() != 201 && res.status() != 409) { // 409 = existiert bereits
            throw new DockerApiException("Netzwerk erstellen: " + NETWORK, res.status(), res.body());
        }
        log.info("Netzwerk '{}' sichergestellt", NETWORK);
    }

    private void ensureEdgeProxy() {
        DockerHttpClient.Response list = docker.get(
                "/containers/json?all=true&filters=" + enc("{\"name\":[\"" + TRAEFIK_NAME + "\"]}"));
        if (list.isSuccess()) {
            for (JsonNode c : readTree(list.body())) {
                if ("running".equals(c.path("State").asText())) {
                    return; // läuft bereits
                }
                docker.delete("/containers/" + c.path("Id").asText() + "?force=true"); // stale → weg
            }
        }
        DockerHttpClient.Response pulled = docker.post(
                "/images/create?fromImage=" + TRAEFIK_IMAGE_REPO + "&tag=" + TRAEFIK_IMAGE_TAG, null);
        if (!pulled.isSuccess()) {
            throw new DockerApiException("Traefik-Image beziehen", pulled.status(), pulled.body());
        }
        if (ingress.tls().acme().enabled()) {
            ensureAcmeVolume();
        }
        DockerHttpClient.Response created = docker.post(
                "/containers/create?name=" + enc(TRAEFIK_NAME), toJson(traefikCreateBody()));
        if (!created.isSuccess()) {
            throw new DockerApiException("Traefik erstellen", created.status(), created.body());
        }
        String id = readTree(created.body()).path("Id").asText();
        DockerHttpClient.Response started = docker.post("/containers/" + id + "/start", null);
        if (started.status() != 204 && started.status() != 304) {
            throw new DockerApiException("Traefik starten", started.status(), started.body());
        }
        log.info("Edge-Proxy (Traefik) gestartet");
    }

    private void ensureAcmeVolume() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Name", ACME_VOLUME);
        body.put("Labels", Map.of(D4yLabels.MANAGED, "true"));
        docker.post("/volumes/create", toJson(body)); // idempotent
    }

    private Map<String, Object> traefikCreateBody() {
        D4yProperties.Acme acme = ingress.tls().acme();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Image", TRAEFIK_IMAGE_REPO + ":" + TRAEFIK_IMAGE_TAG);
        body.put("Cmd", traefikArgs());
        body.put("Labels", Map.of("d4y.system", "edge-proxy"));
        body.put("ExposedPorts", Map.of("80/tcp", Map.of(), "443/tcp", Map.of()));
        if (acme.enabled() && !acme.env().isEmpty()) {
            body.put("Env", acme.env().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList());
        }
        Map<String, Object> hostConfig = new LinkedHashMap<>();
        hostConfig.put("NetworkMode", NETWORK);
        hostConfig.put("Binds", List.of(socketPath + ":/var/run/docker.sock:ro"));
        if (acme.enabled()) {
            hostConfig.put("Mounts", List.of(
                    Map.of("Type", "volume", "Source", ACME_VOLUME, "Target", "/acme")));
        }
        Map<String, Object> ports = new LinkedHashMap<>();
        ports.put("80/tcp", List.of(Map.of("HostPort", "80")));
        ports.put("443/tcp", List.of(Map.of("HostPort", "443")));
        hostConfig.put("PortBindings", ports);
        hostConfig.put("RestartPolicy", Map.of("Name", "unless-stopped"));
        body.put("HostConfig", hostConfig);
        return body;
    }

    /** Baut die Traefik-CLI-Argumente aus der Ingress-Konfiguration (paket-sichtbar für Tests). */
    List<String> traefikArgs() {
        List<String> args = new ArrayList<>(List.of(
                "--providers.docker=true",
                "--providers.docker.exposedbydefault=false",
                "--providers.docker.network=" + NETWORK,
                "--entrypoints.web.address=:80",
                "--entrypoints.websecure.address=:443"));
        if (ingress.httpsRedirect()) {
            args.add("--entrypoints.web.http.redirections.entrypoint.to=websecure");
            args.add("--entrypoints.web.http.redirections.entrypoint.scheme=https");
        }
        D4yProperties.Acme acme = ingress.tls().acme();
        if (acme.enabled()) {
            String r = "--certificatesresolvers." + CERT_RESOLVER + ".acme.";
            args.add(r + "email=" + acme.email());
            args.add(r + "storage=/acme/acme.json");
            if (acme.dnsChallenge()) {
                args.add(r + "dnschallenge=true");
                if (!acme.dnsProvider().isBlank()) {
                    args.add(r + "dnschallenge.provider=" + acme.dnsProvider());
                }
            } else {
                args.add(r + "httpchallenge=true");
                args.add(r + "httpchallenge.entrypoint=web");
            }
            if (!acme.caServer().isBlank()) {
                args.add(r + "caserver=" + acme.caServer());
            }
        }
        return args;
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String toJson(Object body) {
        try {
            return json.writeValueAsString(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode readTree(String body) {
        try {
            return json.readTree(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
