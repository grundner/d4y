package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.TestFixtures;
import io.d4y.domain.model.ContainerSpec;
import io.d4y.domain.model.ImageRef;
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

    /** Zeichnet POST-Aufrufe auf und liefert plausible Engine-Antworten. */
    private static final class CapturingClient extends DockerHttpClient {
        final List<String> paths = new ArrayList<>();
        final Map<String, String> bodies = new LinkedHashMap<>();

        CapturingClient() {
            super(TestFixtures.props());
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
        DockerContainerBackend backend = new DockerContainerBackend(client, json);

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
    void createPayloadHasNoHostConfigWithoutVolumes() throws Exception {
        CapturingClient client = new CapturingClient();
        DockerContainerBackend backend = new DockerContainerBackend(client, json);

        backend.run(ContainerSpec.forApplication(
                new io.d4y.domain.model.Application("nginx", ImageRef.of("nginx:1.27-alpine"))));

        assertThat(client.paths).doesNotContain("/volumes/create");
        JsonNode createBody = json.readTree(client.bodies.get("/containers/create"));
        assertThat(createBody.has("HostConfig")).isFalse();
        assertThat(createBody.path("Labels").path("d4y.volumes").asText()).isEmpty();
    }
}
