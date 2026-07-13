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
        @DefaultValue Ingress ingress,
        @DefaultValue ConfigRepo configRepo,
        @DefaultValue Backup backup,
        @DefaultValue Trigger trigger,
        @DefaultValue Secrets secrets) {

    /** Anbindung an die Docker-Engine (HTTP über Unix-Socket). */
    public record Docker(
            @DefaultValue("/var/run/docker.sock") String socketPath,
            /** Optionaler API-Versions-Präfix, z. B. {@code /v1.43}. Leer = Engine-Default. */
            @DefaultValue("") String apiVersion) {
    }

    /** Lokale Herkunft des Sollzustands (Fallback, wenn kein Git-Config-Repo gesetzt ist). */
    public record DesiredState(@DefaultValue("./desired") String path) {
    }

    /**
     * Git-Config-Repository als Sollzustands-Quelle (ADR-0019). Aktiv, sobald {@code url} gesetzt ist;
     * sonst greift der lokale {@link DesiredState}-Modus.
     *
     * @param path lokaler Unterpfad im Repo mit den YAML-Dateien ({@code ""} = Repo-Wurzel)
     * @param localDir lokales Arbeitsverzeichnis für den Klon
     * @param username/token HTTPS-Zugangsdaten für private Repos (Geheimnisse)
     */
    public record ConfigRepo(@DefaultValue("") String url,
                             @DefaultValue("main") String branch,
                             @DefaultValue("") String path,
                             @DefaultValue("./.d4y-config") String localDir,
                             @DefaultValue("30000") long pollIntervalMs,
                             @DefaultValue("") String username,
                             @DefaultValue("") String token) {

        public boolean enabled() {
            return url != null && !url.isBlank();
        }
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
    public record Ingress(@DefaultValue("d4y.internal") String internalDomain,
                          @DefaultValue("extern") String dnsMode,
                          @DefaultValue Self self,
                          @DefaultValue Tls tls) {

        public boolean managedDns() {
            return "managed".equalsIgnoreCase(dnsMode);
        }
    }

    /**
     * Selbst-Ingress von d4y (ADR-0027). Im Host-Betrieb läuft d4y nicht als Container, daher kann
     * Traefiks Docker-Provider d4ys eigene Route nicht aus Container-Labels lesen. d4y deklariert sie
     * stattdessen über den Traefik-<b>File-Provider</b>: es schreibt {@code d4y.json} nach
     * {@link #dynamicDir()} (in den Traefik-Container als {@code /dynamic} gemountet) mit einem Router
     * {@code Host(host)} und einem Service, der auf {@link #target()} (Host-Gateway) zeigt.
     *
     * @param host   öffentlicher Hostname von d4y (aus {@code D4Y_HOST}); leer ⇒ keine Selbst-Route
     * @param target Backend-URL, unter der Traefik den Host-d4y erreicht (Docker-Host-Gateway)
     * @param dynamicDir Host-Verzeichnis der dynamischen Traefik-Config (Bind-Mount nach {@code /dynamic})
     */
    public record Self(@DefaultValue("") String host,
                       @DefaultValue("http://host.docker.internal:8080") String target,
                       @DefaultValue("/var/lib/d4y/traefik-dynamic") String dynamicDir,
                       Boolean tls) {

        /** Selbst-Route aktiv, sobald ein Host gesetzt ist (Host-Betrieb). */
        public boolean enabled() {
            return host != null && !host.isBlank();
        }

        /** Effektives TLS der Selbst-Route (ADR-0028): explizit, sonst globaler Default. */
        public boolean tlsEnabled(boolean defaultEnabled) {
            return tls != null ? tls : defaultEnabled;
        }
    }

    /**
     * TLS-Konfiguration (ADR-0017/0028). {@code defaultEnabled} steuert das Standard-TLS pro Route und
     * für die Selbst-Route, wenn diese nichts angeben: explizit gesetzt, sonst abgeleitet aus ACME.
     * Ohne ACME-Mail ⇒ {@code false} ⇒ reiner HTTP-Betrieb (VM ohne Public IP).
     */
    public record Tls(Boolean defaultEnabled, @DefaultValue Acme acme) {

        public boolean effectiveDefault() {
            return defaultEnabled != null ? defaultEnabled : acme.enabled();
        }
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

    /**
     * Backup/Restore der Volume-Daten in einen S3-kompatiblen Store (ADR-0020). Aktiv, sobald der
     * S3-Store konfiguriert ist; je App per {@code backup: true} im Desired-State opt-in.
     */
    public record Backup(@DefaultValue("300000") long intervalMs, @DefaultValue S3 s3) {

        public boolean storeConfigured() {
            return s3 != null && s3.configured();
        }
    }

    /** S3-kompatibler Backup-Store. {@code accessKey}/{@code secretKey} sind Geheimnisse. */
    public record S3(@DefaultValue("") String endpoint,
                     @DefaultValue("") String bucket,
                     @DefaultValue("us-east-1") String region,
                     @DefaultValue("Other") String provider,
                     @DefaultValue("") String accessKey,
                     @DefaultValue("") String secretKey) {

        public boolean configured() {
            return endpoint != null && !endpoint.isBlank() && bucket != null && !bucket.isBlank();
        }
    }

    /**
     * Push-Trigger-Endpoint {@code POST /api/reconcile} (ADR-0023). {@code token} ist ein
     * host/d4y-Credential (kein Image-Secret). Leer ⇒ Endpoint deaktiviert (fail-closed).
     */
    public record Trigger(@DefaultValue("") String token) {

        public boolean enabled() {
            return token != null && !token.isBlank();
        }
    }

    /**
     * Verschlüsselter Secret-Store für gelieferte Image/Container-Secrets (ADR-0024).
     * {@code encryptionKey} ist ein host/d4y-Credential; leer ⇒ gelieferte Secrets bleiben nur im RAM
     * (keine Persistenz über Neustarts). {@code file} ist der Ablageort des verschlüsselten Stores.
     */
    public record Secrets(@DefaultValue("") String encryptionKey,
                          @DefaultValue("./.d4y-secrets") String file) {

        public boolean persistent() {
            return encryptionKey != null && !encryptionKey.isBlank();
        }
    }
}
