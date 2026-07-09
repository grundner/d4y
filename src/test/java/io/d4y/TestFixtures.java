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
        return new D4yProperties(
                new D4yProperties.Docker("/var/run/docker.sock", ""),
                new D4yProperties.DesiredState(desiredPath),
                new D4yProperties.Reconcile(15000),
                new D4yProperties.Operations(
                        new D4yProperties.HoldConfig(900, 3600),
                        new D4yProperties.LogsConfig(200)),
                new D4yProperties.Ingress(true,
                        new D4yProperties.Tls(
                                new D4yProperties.Acme("", "http", "", "", Map.of()))));
    }
}
