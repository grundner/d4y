package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.config.D4yProperties;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Prüft die rclone-Helfer-Payloads für Backup/Restore, ohne echte Engine/S3. */
class DockerVolumeBackupTest {

    private final ObjectMapper json = new ObjectMapper();

    private static final class Cap extends DockerHttpClient {
        final Map<String, String> bodies = new LinkedHashMap<>();

        Cap() {
            super(props());
        }

        @Override
        public Response post(String path, String body) {
            if (body != null) {
                bodies.put(path.split("\\?")[0], body);
            }
            if (path.startsWith("/images/create")) {
                return new Response(200, "");
            }
            if (path.startsWith("/containers/create")) {
                return new Response(201, "{\"Id\":\"h1\"}");
            }
            if (path.contains("/wait")) {
                return new Response(200, "{\"StatusCode\":0}");
            }
            if (path.contains("/start")) {
                return new Response(204, "");
            }
            return new Response(200, "");
        }

        @Override
        public Response delete(String path) {
            return new Response(204, "");
        }
    }

    @Test
    void backupSyncsVolumeToStoreWithCredentialsInEnv() throws Exception {
        Cap c = new Cap();
        DockerVolumeBackup vb = new DockerVolumeBackup(c, json, props());
        assertThat(vb.enabled()).isTrue();

        vb.backup("nginx", "html");

        JsonNode body = json.readTree(c.bodies.get("/containers/create"));
        List<String> cmd = new ArrayList<>();
        body.path("Cmd").forEach(e -> cmd.add(e.asText()));
        assertThat(cmd).containsExactly("sync", "/data", "store:mybucket/nginx/html");

        List<String> env = new ArrayList<>();
        body.path("Env").forEach(e -> env.add(e.asText()));
        assertThat(env)
                .contains("RCLONE_CONFIG_STORE_TYPE=s3",
                        "RCLONE_CONFIG_STORE_ENDPOINT=http://minio:9000",
                        "RCLONE_CONFIG_STORE_ACCESS_KEY_ID=key",
                        "RCLONE_CONFIG_STORE_SECRET_ACCESS_KEY=secret");

        // Compose-Volume-Name: <projekt>_<volume> = d4y-<app>_<vol> (ADR-0029).
        assertThat(body.path("HostConfig").path("Mounts").get(0).path("Source").asText())
                .isEqualTo("d4y-nginx_html");
        assertThat(body.path("HostConfig").path("NetworkMode").asText()).isEqualTo("d4y");
    }

    @Test
    void restoreSyncsStoreToVolume() throws Exception {
        Cap c = new Cap();
        new DockerVolumeBackup(c, json, props()).restore("nginx", "html");

        JsonNode body = json.readTree(c.bodies.get("/containers/create"));
        List<String> cmd = new ArrayList<>();
        body.path("Cmd").forEach(e -> cmd.add(e.asText()));
        assertThat(cmd).containsExactly("sync", "store:mybucket/nginx/html", "/data");
    }

    @Test
    void disabledWithoutStore() {
        D4yProperties noStore = withS3(new D4yProperties.S3("", "", "us-east-1", "Other", "", ""));
        assertThat(new DockerVolumeBackup(new Cap(), new ObjectMapper(), noStore).enabled()).isFalse();
    }

    private static D4yProperties props() {
        return withS3(new D4yProperties.S3("http://minio:9000", "mybucket", "us-east-1", "Minio", "key", "secret"));
    }

    private static D4yProperties withS3(D4yProperties.S3 s3) {
        return new D4yProperties(
                new D4yProperties.Docker("/var/run/docker.sock", ""),
                new D4yProperties.DesiredState("./desired"),
                new D4yProperties.Reconcile(15000),
                new D4yProperties.Operations(
                        new D4yProperties.HoldConfig(900, 3600),
                        new D4yProperties.LogsConfig(200)),
                new D4yProperties.Ingress("d4y.internal", "extern",
                        new D4yProperties.Self("", "http://host.docker.internal:8080", "/var/lib/d4y/traefik-dynamic", null),
                        new D4yProperties.Tls(null, new D4yProperties.Acme("", "http", "", "", Map.of()))),
                new D4yProperties.ConfigRepo("", "main", "", "./.d4y-config", 30000, "", ""),
                new D4yProperties.Backup(300000, s3),
                new D4yProperties.Trigger(""),
                new D4yProperties.Secrets("", "./.d4y-secrets"));
    }
}
