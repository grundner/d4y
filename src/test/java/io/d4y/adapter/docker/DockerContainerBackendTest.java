package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.TestFixtures;
import io.d4y.domain.model.Application;
import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.ImageRef;
import io.d4y.domain.model.Route;
import io.d4y.domain.model.VolumeMapping;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prüft die Konstruktion des Container-Create-Payloads — insbesondere die Named-Volume-Mounts —
 * ohne echte Engine, über einen aufzeichnenden {@link DockerHttpClient}.
 */
class DockerContainerBackendTest {

    private final ObjectMapper json = new ObjectMapper();

    /** VolumeBackup-Stub: deaktiviert (kein Restore/Backup im Test). */
    private io.d4y.port.VolumeBackup noBackup() {
        return new io.d4y.port.VolumeBackup() {
            @Override public boolean enabled() { return false; }
            @Override public void backup(String appName, String volumeName) { }
            @Override public void restore(String appName, String volumeName) { }
        };
    }

    /** Edge-Proxy-Stub: ensureNetwork ohne echten Docker-Aufruf. */
    private DockerEdgeProxy edge(DockerHttpClient client) {
        return new DockerEdgeProxy(client, json, TestFixtures.props()) {
            @Override
            public void ensureNetwork() {
                // no-op im Test
            }
        };
    }

    /** Zeichnet POST-Aufrufe auf und liefert plausible Engine-Antworten. */
    private static final class CapturingClient extends DockerHttpClient {
        final List<String> paths = new ArrayList<>();
        final Map<String, String> bodies = new LinkedHashMap<>();

        CapturingClient() {
            super(TestFixtures.props());
        }

        @Override
        public Response get(String path) {
            paths.add(path);
            // Volume existiert noch nicht → wird angelegt (volumeExists → false).
            if (path.startsWith("/volumes/")) {
                return new Response(404, "");
            }
            return new Response(200, "");
        }

        @Override
        public Response post(String path, String jsonBody) {
            paths.add(path);
            if (jsonBody != null) {
                bodies.put(path.split("\\?")[0], jsonBody);
            }
            if (path.startsWith("/images/create")) {
                return new Response(200, "");
            }
            if (path.equals("/volumes/create")) {
                return new Response(201, "{}");
            }
            if (path.startsWith("/containers/create")) {
                return new Response(201, "{\"Id\":\"c123\"}");
            }
            if (path.contains("/start")) {
                return new Response(204, "");
            }
            return new Response(200, "");
        }
    }

    @Test
    void createPayloadMountsNamedVolumeAndEnsuresVolume() throws Exception {
        CapturingClient client = new CapturingClient();
        DockerContainerBackend backend = new DockerContainerBackend(client, json, edge(client), noBackup());

        ContainerSpec spec = new ContainerSpec("nginx", ImageRef.of("nginx:1.27-alpine"), Map.of(),
                List.of(new VolumeMapping("html", "/usr/share/nginx/html")));

        String id = backend.run(spec);

        assertThat(id).isEqualTo("c123");
        assertThat(client.paths).contains("/volumes/create");

        JsonNode volumeBody = json.readTree(client.bodies.get("/volumes/create"));
        assertThat(volumeBody.path("Name").asText()).isEqualTo("d4y_nginx_html");

        JsonNode createBody = json.readTree(client.bodies.get("/containers/create"));
        assertThat(createBody.path("Labels").path("d4y.volumes").asText())
                .isEqualTo("html=/usr/share/nginx/html");

        JsonNode mounts = createBody.path("HostConfig").path("Mounts");
        assertThat(mounts.isArray()).isTrue();
        assertThat(mounts).singleElement().satisfies(m -> {
            assertThat(m.path("Type").asText()).isEqualTo("volume");
            assertThat(m.path("Source").asText()).isEqualTo("d4y_nginx_html");
            assertThat(m.path("Target").asText()).isEqualTo("/usr/share/nginx/html");
        });
    }

    @Test
    void createPayloadStampsEnvAndMergesOverride() throws Exception {
        CapturingClient client = new CapturingClient();
        DockerContainerBackend backend = new DockerContainerBackend(client, json, edge(client), noBackup());

        Application app = new Application("web", ImageRef.of("nginx:1.27-alpine"), List.of(), List.of(),
                Map.of("LOG_LEVEL", "info"));
        // Operativer Override gewinnt über das deklarierte env.
        backend.run(ContainerSpec.forApplication(app, Map.of("LOG_LEVEL", "debug", "EXTRA", "1")));

        JsonNode createBody = json.readTree(client.bodies.get("/containers/create"));
        List<String> env = new ArrayList<>();
        createBody.path("Env").forEach(e -> env.add(e.asText()));
        assertThat(env).contains("LOG_LEVEL=debug", "EXTRA=1");

        JsonNode envLabel = json.readTree(createBody.path("Labels").path("d4y.env").asText());
        assertThat(envLabel.path("LOG_LEVEL").asText()).isEqualTo("debug");
        assertThat(envLabel.path("EXTRA").asText()).isEqualTo("1");
    }

    @Test
    void createPayloadHasNoHostConfigWithoutVolumes() throws Exception {
        CapturingClient client = new CapturingClient();
        DockerContainerBackend backend = new DockerContainerBackend(client, json, edge(client), noBackup());

        backend.run(ContainerSpec.forApplication(
                new io.d4y.domain.model.Application("nginx", ImageRef.of("nginx:1.27-alpine"))));

        assertThat(client.paths).doesNotContain("/volumes/create");
        JsonNode createBody = json.readTree(client.bodies.get("/containers/create"));
        assertThat(createBody.has("HostConfig")).isFalse();
        assertThat(createBody.path("Labels").path("d4y.volumes").asText()).isEmpty();
        // Netzanbindung ist immer gesetzt (gemeinsames d4y-Netz).
        assertThat(createBody.path("NetworkingConfig").path("EndpointsConfig").has("d4y")).isTrue();
    }

    @Test
    void createPayloadAddsTraefikLabelsAndNetworkForRoutes() throws Exception {
        CapturingClient client = new CapturingClient();
        DockerContainerBackend backend = new DockerContainerBackend(client, json, edge(client), noBackup());

        ContainerSpec spec = new ContainerSpec("web", ImageRef.of("nginx:1.27-alpine"), Map.of(),
                List.of(), List.of(new Route("web.example.com", "/", 8080)));

        backend.run(spec);

        JsonNode createBody = json.readTree(client.bodies.get("/containers/create"));
        JsonNode labels = createBody.path("Labels");
        // ADR-0028: Fixture ohne ACME ⇒ TLS-Default aus, Route auf 'web' (reines HTTP).
        assertThat(labels.path("d4y.routes").asText()).isEqualTo("web.example.com|/|8080|");
        assertThat(labels.path("traefik.enable").asText()).isEqualTo("true");
        assertThat(labels.path("traefik.http.routers.d4y-web-0.rule").asText())
                .isEqualTo("Host(`web.example.com`)");
        assertThat(labels.path("traefik.http.routers.d4y-web-0.entrypoints").asText())
                .isEqualTo("web");
        assertThat(labels.path("traefik.http.routers.d4y-web-0.tls").asText()).isEmpty();
        assertThat(labels.path("traefik.http.services.d4y-web-0.loadbalancer.server.port").asText())
                .isEqualTo("8080");
        // Ziel-Alias im d4y-Netz = App-Name.
        assertThat(createBody.path("NetworkingConfig").path("EndpointsConfig").path("d4y")
                .path("Aliases").get(0).asText()).isEqualTo("web");
    }

    @Test
    void perRouteTlsTrueUsesWebsecureAndTls() throws Exception {
        CapturingClient client = new CapturingClient();
        DockerContainerBackend backend = new DockerContainerBackend(client, json, edge(client), noBackup());

        // Zwei Routen: eine explizit HTTPS (tls=true), eine explizit HTTP (tls=false) — ADR-0028.
        ContainerSpec spec = new ContainerSpec("web", ImageRef.of("nginx:1.27-alpine"), Map.of(),
                List.of(), List.of(
                        new Route("secure.example.com", "/", 8080, Boolean.TRUE),
                        new Route("plain.example.com", "/", 8080, Boolean.FALSE)));

        backend.run(spec);

        JsonNode labels = json.readTree(client.bodies.get("/containers/create")).path("Labels");
        assertThat(labels.path("traefik.http.routers.d4y-web-0.entrypoints").asText())
                .isEqualTo("websecure");
        assertThat(labels.path("traefik.http.routers.d4y-web-0.tls").asText()).isEqualTo("true");
        assertThat(labels.path("traefik.http.routers.d4y-web-1.entrypoints").asText())
                .isEqualTo("web");
        assertThat(labels.path("traefik.http.routers.d4y-web-1.tls").asText()).isEmpty();
    }
}
