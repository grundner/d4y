package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.config.D4yProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Prüft Entrypoint/TLS-Ableitung, Traefik-Argumente und die Selbst-Route (ohne echte Engine). */
class DockerEdgeProxyTest {

    private static final String DYN = "/var/lib/d4y/traefik-dynamic";

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

    private static D4yProperties.Ingress ingress(D4yProperties.Acme acme) {
        return ingress(acme, null, defaultSelf());
    }

    private static D4yProperties.Ingress ingress(D4yProperties.Acme acme, Boolean tlsDefault,
                                                 D4yProperties.Self self) {
        return new D4yProperties.Ingress("d4y.internal", "extern",
                self, new D4yProperties.Tls(tlsDefault, acme));
    }

    private static D4yProperties.Self defaultSelf() {
        return new D4yProperties.Self("", "http://host.docker.internal:8080", DYN, null);
    }

    private static D4yProperties.Self self(String host, String dynamicDir, Boolean tls) {
        return new D4yProperties.Self(host, "http://host.docker.internal:8080", dynamicDir, tls);
    }

    private static D4yProperties.Acme acme(String email, String challenge, String dnsProvider) {
        return new D4yProperties.Acme(email, challenge, dnsProvider, "", Map.of());
    }

    @Test
    void withoutAcmeDefaultTlsIsOffAndNoResolver() {
        DockerEdgeProxy p = proxy(ingress(acme("", "http", "")));

        assertThat(p.defaultTlsEnabled()).isFalse();
        assertThat(p.certResolver()).isNull();
        assertThat(p.entrypointForTls(true)).isEqualTo("websecure");
        assertThat(p.entrypointForTls(false)).isEqualTo("web");
    }

    @Test
    void acmeMakesTlsDefaultOnAndResolverLe() {
        DockerEdgeProxy p = proxy(ingress(acme("ops@example.com", "http", "")));

        assertThat(p.defaultTlsEnabled()).isTrue();
        assertThat(p.certResolver()).isEqualTo("le");
    }

    @Test
    void explicitTlsDefaultOverridesAcmeDerivation() {
        // Ohne ACME, aber TLS-Default explizit an ⇒ self-signed HTTPS als Default.
        DockerEdgeProxy p = proxy(ingress(acme("", "http", ""), Boolean.TRUE, defaultSelf()));

        assertThat(p.defaultTlsEnabled()).isTrue();
        assertThat(p.certResolver()).isNull();
    }

    @Test
    void noGlobalHttpsRedirectInTraefikArgs() {
        DockerEdgeProxy p = proxy(ingress(acme("ops@example.com", "http", "")));

        assertThat(p.traefikArgs())
                .contains("--entrypoints.web.address=:80", "--entrypoints.websecure.address=:443")
                .noneMatch(a -> a.contains("redirections"));
    }

    @Test
    void acmeHttpChallengeAddsResolver() {
        DockerEdgeProxy p = proxy(ingress(acme("ops@example.com", "http", "")));

        assertThat(p.traefikArgs())
                .contains("--certificatesresolvers.le.acme.email=ops@example.com",
                        "--certificatesresolvers.le.acme.httpchallenge=true",
                        "--certificatesresolvers.le.acme.storage=/acme/acme.json");
        assertThat(p.certResolver()).isEqualTo("le");
    }

    @Test
    void acmeDnsChallengeAddsProvider() {
        DockerEdgeProxy p = proxy(ingress(acme("ops@example.com", "dns", "cloudflare")));

        assertThat(p.traefikArgs())
                .contains("--certificatesresolvers.le.acme.dnschallenge=true",
                        "--certificatesresolvers.le.acme.dnschallenge.provider=cloudflare")
                .noneMatch(a -> a.contains("httpchallenge"));
    }

    @Test
    void networkAliasesIncludeAppNameAndInternalFqdn() {
        DockerEdgeProxy p = proxy(ingress(acme("", "http", "")));

        assertThat(p.networkAliases("nginx")).containsExactly("nginx", "nginx.d4y.internal");
    }

    @Test
    void selfDisabledHasNoFileProvider() {
        DockerEdgeProxy p = proxy(ingress(acme("", "http", "")));

        assertThat(p.traefikArgs()).noneMatch(a -> a.contains("providers.file"));
    }

    @Test
    void selfEnabledAddsFileProvider() {
        DockerEdgeProxy p = proxy(ingress(acme("ops@example.com", "http", ""), null,
                self("d4y.example.com", DYN, null)));

        assertThat(p.traefikArgs())
                .contains("--providers.file.directory=/dynamic", "--providers.file.watch=true");
    }

    @Test
    void writeSelfRouteHttpsWithAcme(@TempDir Path dir) throws Exception {
        DockerEdgeProxy p = proxy(ingress(acme("ops@example.com", "http", ""), null,
                self("d4y.example.com", dir.toString(), null)));

        p.writeSelfRoute();

        String content = Files.readString(dir.resolve("d4y.json"));
        assertThat(content)
                .contains("Host(`d4y.example.com`)")
                .contains("http://host.docker.internal:8080")
                .contains("websecure")
                .contains("\"certResolver\" : \"le\"");
    }

    @Test
    void writeSelfRouteHttpWithoutAcme(@TempDir Path dir) throws Exception {
        // Kein ACME ⇒ HTTP-only: Selbst-Route auf 'web', ohne tls-Key (ADR-0028).
        DockerEdgeProxy p = proxy(ingress(acme("", "http", ""), null,
                self("d4y.local", dir.toString(), null)));

        p.writeSelfRoute();

        String content = Files.readString(dir.resolve("d4y.json"));
        assertThat(content)
                .contains("Host(`d4y.local`)")
                .contains("\"web\"")
                .doesNotContain("websecure")
                .doesNotContain("tls");
    }

    @Test
    void writeSelfRouteNoopWhenHostBlank(@TempDir Path dir) {
        DockerEdgeProxy p = proxy(ingress(acme("", "http", ""), null, self("", dir.toString(), null)));

        p.writeSelfRoute();

        assertThat(dir.resolve("d4y.json")).doesNotExist();
    }
}
