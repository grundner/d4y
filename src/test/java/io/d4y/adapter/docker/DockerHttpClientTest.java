package io.d4y.adapter.docker;

import io.d4y.config.D4yProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockerHttpClientTest {

    private static DockerHttpClient clientFor(String socketPath) {
        D4yProperties props = new D4yProperties(
                new D4yProperties.Docker(socketPath, ""),
                new D4yProperties.DesiredState("./desired"),
                new D4yProperties.Reconcile(15000),
                new D4yProperties.Operations(
                        new D4yProperties.HoldConfig(900, 3600),
                        new D4yProperties.LogsConfig(200)),
                new D4yProperties.Ingress(true, "d4y.internal", "extern",
                        new D4yProperties.Tls(
                                new D4yProperties.Acme("", "http", "", "", java.util.Map.of()))),
                new D4yProperties.ConfigRepo("", "main", "", "./.d4y-config", 30000, "", ""));
        return new DockerHttpClient(props);
    }

    @Test
    void translatesUnreachableSocketToDockerUnavailable() {
        // Nicht existierender Socket-Pfad ⇒ Verbindungsaufbau scheitert (wie „Docker läuft nicht").
        DockerHttpClient client = clientFor("/tmp/d4y-does-not-exist-" + getClass().getName() + ".sock");

        assertThatThrownBy(() -> client.get("/version"))
                .isInstanceOf(DockerUnavailableException.class)
                .hasMessageContaining("nicht erreichbar");
    }

    @Test
    void unavailableMessageNamesTheSocketPath() {
        String socket = "/tmp/d4y-missing.sock";
        DockerHttpClient client = clientFor(socket);

        assertThatThrownBy(() -> client.post("/containers/create", "{}"))
                .isInstanceOf(DockerUnavailableException.class)
                .hasMessageContaining(socket);

        assertThat(new DockerUnavailableException(socket, new java.io.FileNotFoundException()).getMessage())
                .contains(socket)
                .contains("FileNotFoundException");
    }
}
