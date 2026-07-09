package io.d4y.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

/**
 * Zentrale Konfiguration von D4Y (Präfix {@code d4y}).
 *
 * <p>Server sind austauschbar und tragen keine individuelle Konfiguration; diese Werte
 * beschreiben lediglich, wie das Backend mit der Engine und der Desired-State-Quelle spricht.
 */
@ConfigurationProperties(prefix = "d4y")
public record D4yProperties(
        @DefaultValue Docker docker,
        @DefaultValue DesiredState desiredState,
        @DefaultValue Reconcile reconcile,
        @DefaultValue Operations operations,
        @DefaultValue Ingress ingress) {

    /** Anbindung an die Docker-Engine (HTTP über Unix-Socket). */
    public record Docker(
            @DefaultValue("/var/run/docker.sock") String socketPath,
            /** Optionaler API-Versions-Präfix, z. B. {@code /v1.43}. Leer = Engine-Default. */
            @DefaultValue("") String apiVersion) {
    }

    /** Herkunft des Sollzustands. ADR-0011 (Interim): lokales Verzeichnis, später Git. */
    public record DesiredState(@DefaultValue("./desired") String path) {
    }

    /** Steuerung des Reconciliation-Loops. */
    public record Reconcile(@DefaultValue("15000") long intervalMs) {
    }

    /** Operative Aktionen und Hold (ADR-0013). */
    public record Operations(@DefaultValue HoldConfig hold, @DefaultValue LogsConfig logs) {
    }

    public record HoldConfig(
            @DefaultValue("900") long defaultSeconds,
            @DefaultValue("3600") long maxSeconds) {
    }

    public record LogsConfig(@DefaultValue("200") int defaultTail) {
    }

    /**
     * Ingress/TLS/Netzwerk-Konfiguration (ADR-0016/0017/0018).
     *
     * @param internalDomain interne DNS-Domain für Service-Discovery-Aliase (`<app>.<domain>`)
     * @param dnsMode öffentliche DNS-Verwaltung: {@code extern} (Default) oder {@code managed}
     */
    public record Ingress(@DefaultValue("true") boolean httpsRedirect,
                          @DefaultValue("d4y.internal") String internalDomain,
                          @DefaultValue("extern") String dnsMode,
                          @DefaultValue Tls tls) {

        public boolean managedDns() {
            return "managed".equalsIgnoreCase(dnsMode);
        }
    }

    public record Tls(@DefaultValue Acme acme) {
    }

    /**
     * ACME/Let's-Encrypt-Konfiguration. Aktiv, sobald {@code email} gesetzt ist; sonst nutzt Traefik
     * sein self-signed Default-Zertifikat.
     *
     * @param challenge {@code http} oder {@code dns}
     * @param dnsProvider Traefik-DNS-Provider-Name (nur bei {@code challenge=dns})
     * @param env Zugangsdaten des DNS-Providers als Container-Env (z. B. {@code CF_DNS_API_TOKEN})
     * @param caServer optionaler alternativer ACME-CA-Server (z. B. Staging)
     */
    public record Acme(@DefaultValue("") String email,
                       @DefaultValue("http") String challenge,
                       @DefaultValue("") String dnsProvider,
                       @DefaultValue("") String caServer,
                       Map<String, String> env) {

        public Acme {
            env = env == null ? Map.of() : Map.copyOf(env);
        }

        /** ACME ist aktiv, sobald eine E-Mail konfiguriert ist. */
        public boolean enabled() {
            return email != null && !email.isBlank();
        }

        public boolean dnsChallenge() {
            return "dns".equalsIgnoreCase(challenge);
        }
    }
}
