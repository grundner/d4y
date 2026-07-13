package io.d4y;

import io.d4y.config.D4yProperties;

import java.util.Map;

/** Gemeinsame Test-Hilfen. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static D4yProperties props() {
        return props("./desired");
    }

    public static D4yProperties props(String desiredPath) {
        return props(desiredPath, new D4yProperties.Secrets("", "./.d4y-secrets"));
    }

    public static D4yProperties props(String desiredPath, D4yProperties.Secrets secrets) {
        return props(desiredPath, new D4yProperties.Trigger(""), secrets);
    }

    public static D4yProperties props(String desiredPath, D4yProperties.Trigger trigger,
                                      D4yProperties.Secrets secrets) {
        return new D4yProperties(
                new D4yProperties.Docker("/var/run/docker.sock", ""),
                new D4yProperties.DesiredState(desiredPath),
                new D4yProperties.Reconcile(15000),
                new D4yProperties.Operations(
                        new D4yProperties.HoldConfig(900, 3600),
                        new D4yProperties.LogsConfig(200)),
                new D4yProperties.Ingress("d4y.internal", "extern",
                        new D4yProperties.Self("", "http://host.docker.internal:8080", "/var/lib/d4y/traefik-dynamic", null),
                        new D4yProperties.Tls(null,
                                new D4yProperties.Acme("", "http", "", "", Map.of()))),
                new D4yProperties.ConfigRepo("", "main", "", "./.d4y-config", 30000, "", ""),
                new D4yProperties.Backup(300000,
                        new D4yProperties.S3("", "", "us-east-1", "Other", "", "")),
                trigger,
                secrets);
    }
}
