package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.config.D4yProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Prüft die Traefik-CLI-Argumente je TLS-Konfiguration (ohne echte Engine). */
class DockerEdgeProxyTest {

    private static DockerEdgeProxy proxy(D4yProperties.Ingress ingress) {
        D4yProperties props = new D4yProperties(
                new D4yProperties.Docker("/var/run/docker.sock", ""),
                new D4yProperties.DesiredState("./desired"),
                new D4yProperties.Reconcile(15000),
                new D4yProperties.Operations(
                        new D4yProperties.HoldConfig(900, 3600),
                        new D4yProperties.LogsConfig(200)),
                ingress,
                new D4yProperties.ConfigRepo("", "main", "", "./.d4y-config", 30000, "", ""),
                new D4yProperties.Backup(300000,
                        new D4yProperties.S3("", "", "us-east-1", "Other", "", "")),
                new D4yProperties.Trigger(""),
                new D4yProperties.Secrets("", "./.d4y-secrets"));
        return new DockerEdgeProxy(new DockerHttpClient(props), new ObjectMapper(), props);
    }

    private static D4yProperties.Ingress ingress(boolean redirect, D4yProperties.Acme acme) {
        return new D4yProperties.Ingress(redirect, "d4y.internal", "extern",
                new D4yProperties.Self("", "http://host.docker.internal:8080", "/var/lib/d4y/traefik-dynamic"),
                new D4yProperties.Tls(acme));
    }

    private static D4yProperties.Ingress ingressWithSelf(String host, String dynamicDir, D4yProperties.Acme acme) {
        return new D4yProperties.Ingress(true, "d4y.internal", "extern",
                new D4yProperties.Self(host, "http://host.docker.internal:8080", dynamicDir),
                new D4yProperties.Tls(acme));
    }

    private static D4yProperties.Acme acme(String email, String challenge, String dnsProvider) {
        return new D4yProperties.Acme(email, challenge, dnsProvider, "", Map.of());
    }

    @Test
    void selfSignedHasHttpsEntrypointAndRedirectNoResolver() {
        DockerEdgeProxy p = proxy(ingress(true, acme("", "http", "")));

        assertThat(p.traefikArgs())
                .contains("--entrypoints.websecure.address=:443",
                        "--entrypoints.web.http.redirections.entrypoint.to=websecure")
                .noneMatch(a -> a.contains("acme"));
        assertThat(p.routerEntrypoints()).isEqualTo("websecure");
        assertThat(p.certResolver()).isNull();
    }

    @Test
    void redirectOffServesBothEntrypoints() {
        DockerEdgeProxy p = proxy(ingress(false, acme("", "http", "")));

        assertThat(p.traefikArgs()).noneMatch(a -> a.contains("redirections"));
        assertThat(p.routerEntrypoints()).isEqualTo("web,websecure");
    }

    @Test
    void acmeHttpChallengeAddsResolver() {
        DockerEdgeProxy p = proxy(ingress(true, acme("ops@example.com", "http", "")));

        assertThat(p.traefikArgs())
                .contains("--certificatesresolvers.le.acme.email=ops@example.com",
                        "--certificatesresolvers.le.acme.httpchallenge=true",
                        "--certificatesresolvers.le.acme.storage=/acme/acme.json");
        assertThat(p.certResolver()).isEqualTo("le");
    }

    @Test
    void networkAliasesIncludeAppNameAndInternalFqdn() {
        DockerEdgeProxy p = proxy(ingress(true, acme("", "http", "")));

        assertThat(p.networkAliases("nginx")).containsExactly("nginx", "nginx.d4y.internal");
    }

    @Test
    void selfDisabledHasNoFileProvider() {
        DockerEdgeProxy p = proxy(ingress(true, acme("", "http", "")));

        assertThat(p.traefikArgs()).noneMatch(a -> a.contains("providers.file"));
    }

    @Test
    void selfEnabledAddsFileProvider() {
        DockerEdgeProxy p = proxy(ingressWithSelf("d4y.example.com", "/var/lib/d4y/traefik-dynamic",
                acme("ops@example.com", "http", "")));

        assertThat(p.traefikArgs())
                .contains("--providers.file.directory=/dynamic", "--providers.file.watch=true");
    }

    @Test
    void writeSelfRouteWritesDynamicConfig(@TempDir Path dir) throws Exception {
        DockerEdgeProxy p = proxy(ingressWithSelf("d4y.example.com", dir.toString(),
                acme("ops@example.com", "http", "")));

        p.writeSelfRoute();

        Path file = dir.resolve("d4y.json");
        assertThat(file).exists();
        String content = Files.readString(file);
        assertThat(content)
                .contains("Host(`d4y.example.com`)")
                .contains("http://host.docker.internal:8080")
                .contains("\"certResolver\" : \"le\"");
    }

    @Test
    void writeSelfRouteNoopWhenHostBlank(@TempDir Path dir) {
        DockerEdgeProxy p = proxy(ingressWithSelf("", dir.toString(), acme("", "http", "")));

        p.writeSelfRoute();

        assertThat(dir.resolve("d4y.json")).doesNotExist();
    }

    @Test
    void acmeDnsChallengeAddsProvider() {
        DockerEdgeProxy p = proxy(ingress(true, acme("ops@example.com", "dns", "cloudflare")));

        assertThat(p.traefikArgs())
                .contains("--certificatesresolvers.le.acme.dnschallenge=true",
                        "--certificatesresolvers.le.acme.dnschallenge.provider=cloudflare")
                .noneMatch(a -> a.contains("httpchallenge"));
    }
}
