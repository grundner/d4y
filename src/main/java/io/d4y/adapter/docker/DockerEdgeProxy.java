package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
    // Traefiks File-Provider liest nur YAML/TOML (kein JSON) — die Selbst-Route wird daher als YAML geschrieben.
    private final ObjectMapper yaml = new YAMLMapper();
    private final String socketPath;
    private final D4yProperties.Ingress ingress;

    public DockerEdgeProxy(DockerHttpClient docker, ObjectMapper json, D4yProperties properties) {
        this.docker = docker;
        this.json = json;
        this.socketPath = properties.docker().socketPath();
        this.ingress = properties.ingress();
    }

    /** Effektiver globaler TLS-Default pro Route/Selbst-Route (ADR-0028; abgeleitet aus ACME). */
    public boolean defaultTlsEnabled() {
        return ingress.tls().effectiveDefault();
    }

    /** Traefik-Entrypoint einer Route je nach TLS: {@code websecure} (HTTPS) oder {@code web} (HTTP). */
    public String entrypointForTls(boolean tls) {
        return tls ? "websecure" : "web";
    }

    /** Cert-Resolver-Name für {@code tls.certresolver}, oder {@code null} bei self-signed. */
    public String certResolver() {
        return ingress.tls().acme().enabled() ? CERT_RESOLVER : null;
    }

    /**
     * Netz-Aliase für die interne Service-Discovery: der App-Name und der stabile FQDN
     * {@code <app>.<internal-domain>} (ADR-0018).
     */
    public List<String> networkAliases(String appName) {
        return List.of(appName, appName + "." + ingress.internalDomain());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (ingress.managedDns()) {
            log.warn("DNS-Modus 'managed' ist noch nicht implementiert (ADR-0018) — Verhalten wie 'extern'.");
        }
        ensure();
    }

    @Scheduled(fixedDelayString = "${d4y.reconcile.interval-ms}")
    public void periodic() {
        ensure();
    }

    /** Idempotent: Selbst-Route, Netzwerk und Edge-Proxy sicherstellen. Fehler werden geloggt. */
    public synchronized void ensure() {
        try {
            writeSelfRoute();
            ensureNetwork();
            ensureEdgeProxy();
        } catch (RuntimeException e) {
            log.warn("Edge-Proxy/Netz konnte nicht sichergestellt werden: {}", e.getMessage());
            log.debug("Details", e);
        }
    }

    /**
     * ADR-0027: Schreibt d4ys eigene Traefik-Route als dynamische File-Provider-Config. Im Host-Betrieb
     * ist d4y kein Container mit Labels, daher wird die eigene Route über eine Datei deklariert
     * (Ziel = Host-Gateway). Idempotent; ohne gesetzten Host ({@code d4y.ingress.self.host}) ein No-op.
     * Schreibt nur bei geänderter Config, um unnötige Traefik-Reloads zu vermeiden.
     */
    void writeSelfRoute() {
        D4yProperties.Self self = ingress.self();
        if (!self.enabled()) {
            return;
        }
        boolean tlsOn = self.tlsEnabled(defaultTlsEnabled());
        Map<String, Object> router = new LinkedHashMap<>();
        router.put("rule", "Host(`" + self.host() + "`)");
        router.put("entryPoints", List.of(entrypointForTls(tlsOn)));
        router.put("service", "d4y");
        if (tlsOn) {
            Map<String, Object> tls = new LinkedHashMap<>();
            String resolver = certResolver();
            if (resolver != null) {
                tls.put("certResolver", resolver);
            }
            router.put("tls", tls); // leeres tls ⇒ HTTPS mit Default-Zertifikat (self-signed)
        }
        // ADR-0028: ohne TLS bleibt die Selbst-Route reines HTTP (web) — kein tls-Key.
        Map<String, Object> service = Map.of("loadBalancer",
                Map.of("servers", List.of(Map.of("url", self.target()))));
        Map<String, Object> doc = Map.of("http", Map.of(
                "routers", Map.of("d4y", router),
                "services", Map.of("d4y", service)));
        try {
            String content = yaml.writeValueAsString(doc);
            Path dir = Path.of(self.dynamicDir());
            Files.createDirectories(dir);
            Files.deleteIfExists(dir.resolve("d4y.json")); // veraltetes JSON aus früheren Versionen entfernen
            Path file = dir.resolve("d4y.yml");
            if (Files.exists(file) && content.equals(Files.readString(file))) {
                return; // unverändert — kein Reload triggern
            }
            Path tmp = dir.resolve("d4y.yml.tmp");
            Files.writeString(tmp, content);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info("Selbst-Route für Host '{}' → {} geschrieben", self.host(), self.target());
        } catch (IOException e) {
            log.warn("Selbst-Route konnte nicht geschrieben werden ({}): {}", self.dynamicDir(), e.getMessage());
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
        List<String> env = new ArrayList<>();
        // Traefiks Docker-Client spricht sonst eine zu alte API-Version (1.24); moderne Daemons
        // verlangen eine Mindest-API. 1.40 (Docker ≥ 19.03) wird von allen betroffenen Daemons akzeptiert.
        env.add("DOCKER_API_VERSION=1.40");
        if (acme.enabled() && !acme.env().isEmpty()) {
            acme.env().forEach((k, v) -> env.add(k + "=" + v));
        }
        body.put("Env", env);
        Map<String, Object> hostConfig = new LinkedHashMap<>();
        hostConfig.put("NetworkMode", NETWORK);
        List<String> binds = new ArrayList<>();
        binds.add(socketPath + ":/var/run/docker.sock:ro");
        if (ingress.self().enabled()) {
            // ADR-0027: dynamische Config (Selbst-Route) + Host-Gateway-Auflösung für den Host-d4y.
            binds.add(ingress.self().dynamicDir() + ":/dynamic:ro");
            hostConfig.put("ExtraHosts", List.of("host.docker.internal:host-gateway"));
        }
        hostConfig.put("Binds", binds);
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
        if (ingress.self().enabled()) {
            // ADR-0027: File-Provider für d4ys eigene, container-lose Route (Apps bleiben Docker-Provider).
            args.add("--providers.file.directory=/dynamic");
            args.add("--providers.file.watch=true");
        }
        // ADR-0028: kein globaler HTTP→HTTPS-Redirect mehr — TLS wird pro Route/Selbst-Route gewählt.
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
