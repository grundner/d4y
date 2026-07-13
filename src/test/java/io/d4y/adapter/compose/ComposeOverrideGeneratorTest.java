package io.d4y.adapter.compose;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.adapter.docker.DockerEdgeProxy;
import io.d4y.adapter.docker.DockerHttpClient;
import io.d4y.config.D4yProperties;
import io.d4y.domain.model.AppProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Prüft das generierte Override: Netz-Beitritt + Traefik-Labels aus der Sidecar d4y.yaml (ADR-0029). */
class ComposeOverrideGeneratorTest {

    private static DockerEdgeProxy edgeProxy(String acmeEmail) {
        D4yProperties props = new D4yProperties(
                new D4yProperties.Docker("/var/run/docker.sock", ""),
                new D4yProperties.DesiredState("./desired"),
                new D4yProperties.Reconcile(15000),
                new D4yProperties.Operations(
                        new D4yProperties.HoldConfig(900, 3600),
                        new D4yProperties.LogsConfig(200)),
                new D4yProperties.Ingress("d4y.internal", "extern",
                        new D4yProperties.Self("", "http://host.docker.internal:8080", "/tmp/dyn", null),
                        new D4yProperties.Tls(null, new D4yProperties.Acme(acmeEmail, "http", "", "", Map.of()))),
                new D4yProperties.ConfigRepo("", "main", "", "./.d4y-config", 30000, "", ""),
                new D4yProperties.Backup(300000,
                        new D4yProperties.S3("", "", "us-east-1", "Other", "", "")),
                new D4yProperties.Trigger(""),
                new D4yProperties.Secrets("", "./.d4y-secrets"));
        return new DockerEdgeProxy(new DockerHttpClient(props), new ObjectMapper(), props);
    }

    private static AppProject app(Path dir, String compose, String sidecar) throws Exception {
        Files.createDirectories(dir);
        Path composeFile = dir.resolve("compose.yaml");
        Files.writeString(composeFile, compose);
        Path sidecarFile = null;
        if (sidecar != null) {
            sidecarFile = dir.resolve("d4y.yaml");
            Files.writeString(sidecarFile, sidecar);
        }
        return new AppProject(dir.getFileName().toString(), dir, composeFile, sidecarFile);
    }

    @Test
    void attachesNetworkAndRendersHttpsRouteWithAcme(@TempDir Path tmp) throws Exception {
        Path work = tmp.resolve("work");
        ComposeOverrideGenerator gen = new ComposeOverrideGenerator(work.toString(), edgeProxy("ops@example.com"));
        AppProject project = app(tmp.resolve("web"),
                "services:\n  web:\n    image: nginx:1.27-alpine\n",
                "routes:\n  - service: web\n    host: web.d4y.test\n    port: 80\n    tls: true\n");

        Path override = gen.generate(project);
        String yaml = Files.readString(override);

        assertThat(yaml)
                .contains("external: true")                     // d4y-Netz extern
                .contains("\"d4y\"")                             // Service tritt bei
                .contains("traefik.enable")
                .contains("Host(`web.d4y.test`)")
                .contains("websecure")                          // tls=true ⇒ websecure
                .contains("\"true\"")                           // router.tls
                .contains("certresolver")                       // ACME aktiv ⇒ le
                .contains("loadbalancer.server.port");
    }

    @Test
    void withoutAcmeRouteIsHttp(@TempDir Path tmp) throws Exception {
        Path work = tmp.resolve("work");
        ComposeOverrideGenerator gen = new ComposeOverrideGenerator(work.toString(), edgeProxy(""));
        AppProject project = app(tmp.resolve("web"),
                "services:\n  web:\n    image: nginx:1.27-alpine\n",
                "routes:\n  - service: web\n    host: web.d4y.test\n");

        String yaml = Files.readString(gen.generate(project));

        assertThat(yaml)
                .contains("Host(`web.d4y.test`)")
                .contains("\"web\"")                            // web-Entrypoint (HTTP-only Default)
                .doesNotContain("websecure")
                .doesNotContain("certresolver");
    }

    @Test
    void noSidecarStillAttachesNetwork(@TempDir Path tmp) throws Exception {
        Path work = tmp.resolve("work");
        ComposeOverrideGenerator gen = new ComposeOverrideGenerator(work.toString(), edgeProxy(""));
        AppProject project = app(tmp.resolve("cache"),
                "services:\n  redis:\n    image: redis:7-alpine\n", null);

        String yaml = Files.readString(gen.generate(project));

        assertThat(yaml)
                .contains("external: true")
                .contains("\"d4y\"")
                .doesNotContain("traefik");                     // keine Routes ⇒ keine Traefik-Labels
    }
}
