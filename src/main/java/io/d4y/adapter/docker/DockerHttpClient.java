package io.d4y.adapter.docker;

import io.d4y.config.D4yProperties;
import io.netty.channel.unix.DomainSocketAddress;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Dünner HTTP-Client, der über den Unix-Domain-Socket direkt mit der Docker-Engine-API spricht.
 *
 * <p>Nutzt reactor-netty mit nativem Transport (kqueue/epoll). Da der Reconciliation-Loop
 * synchron arbeitet, werden die Aufrufe blockierend ausgeführt.
 */
@Component
public class DockerHttpClient {

    /** Antwort der Engine: HTTP-Status und Rohtext-Body. */
    public record Response(int status, String body) {
        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }

    /** Antwort der Engine als Rohbytes (für multiplexte Streams: Logs/exec). */
    public record ResponseBytes(int status, byte[] body) {
        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }

    private final HttpClient client;
    private final String baseUrl;
    private final Duration timeout = Duration.ofSeconds(120);

    public DockerHttpClient(D4yProperties properties) {
        String socketPath = properties.docker().socketPath();
        this.client = HttpClient.create()
                .remoteAddress(() -> new DomainSocketAddress(socketPath));
        // Wichtig: Über den Domain-Socket MUSS die URI relativ sein (nur Pfad). Eine absolute
        // URL mit Host würde reactor-netty stattdessen per TCP zu diesem Host verbinden lassen.
        this.baseUrl = properties.docker().apiVersion();
    }

    public Response get(String path) {
        return client.get().uri(baseUrl + path)
                .responseSingle(this::toResponse)
                .block(timeout);
    }

    public Response post(String path, String jsonBody) {
        HttpClient.ResponseReceiver<?> receiver;
        if (jsonBody == null) {
            receiver = client.post().uri(baseUrl + path);
        } else {
            receiver = client
                    .headers(h -> h.set("Content-Type", "application/json"))
                    .post().uri(baseUrl + path)
                    .send(ByteBufFlux.fromString(Mono.just(jsonBody)));
        }
        return receiver.responseSingle(this::toResponse).block(timeout);
    }

    public Response delete(String path) {
        return client.delete().uri(baseUrl + path)
                .responseSingle(this::toResponse)
                .block(timeout);
    }

    /** GET, das den Body als Rohbytes liefert (Logs). */
    public ResponseBytes getBytes(String path) {
        return client.get().uri(baseUrl + path)
                .responseSingle(this::toResponseBytes)
                .block(timeout);
    }

    /** POST mit JSON-Body, das den Body als Rohbytes liefert (exec-Start). */
    public ResponseBytes postBytes(String path, String jsonBody) {
        HttpClient.ResponseReceiver<?> receiver;
        if (jsonBody == null) {
            receiver = client.post().uri(baseUrl + path);
        } else {
            receiver = client
                    .headers(h -> h.set("Content-Type", "application/json"))
                    .post().uri(baseUrl + path)
                    .send(ByteBufFlux.fromString(Mono.just(jsonBody)));
        }
        return receiver.responseSingle(this::toResponseBytes).block(timeout);
    }

    private Mono<ResponseBytes> toResponseBytes(reactor.netty.http.client.HttpClientResponse res,
                                                reactor.netty.ByteBufMono content) {
        return content.asByteArray()
                .defaultIfEmpty(new byte[0])
                .map(body -> new ResponseBytes(res.status().code(), body));
    }

    private Mono<Response> toResponse(reactor.netty.http.client.HttpClientResponse res,
                                      reactor.netty.ByteBufMono content) {
        return content.asString(StandardCharsets.UTF_8)
                .defaultIfEmpty("")
                .map(body -> new Response(res.status().code(), body));
    }
}
