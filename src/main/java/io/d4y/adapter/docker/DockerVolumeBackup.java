package io.d4y.adapter.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.d4y.config.D4yProperties;
import io.d4y.port.VolumeBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backup/Restore von Named Volumes in einen S3-kompatiblen Store (ADR-0020) über kurzlebige
 * {@code rclone}-Helfer-Container. Die Volume-Daten fließen nie durch das Backend; S3-Credentials
 * werden ausschließlich als Container-Env übergeben (nie geloggt).
 */
@Component
public class DockerVolumeBackup implements VolumeBackup {

    private static final String RCLONE_REPO = "rclone/rclone";
    private static final String RCLONE_TAG = "latest";
    private static final String REMOTE = "store";
    private static final Logger log = LoggerFactory.getLogger(DockerVolumeBackup.class);

    private final DockerHttpClient docker;
    private final ObjectMapper json;
    private final D4yProperties.Backup cfg;

    public DockerVolumeBackup(DockerHttpClient docker, ObjectMapper json, D4yProperties properties) {
        this.docker = docker;
        this.json = json;
        this.cfg = properties.backup();
    }

    @Override
    public boolean enabled() {
        return cfg.storeConfigured();
    }

    @Override
    public void backup(String appName, String volumeName) {
        // Verzeichnis-Sync: Volume → Store.
        sync(appName, volumeName, "/data", remotePath(appName, volumeName), "Backup");
    }

    @Override
    public void restore(String appName, String volumeName) {
        // Verzeichnis-Sync: Store → Volume.
        sync(appName, volumeName, remotePath(appName, volumeName), "/data", "Restore");
    }

    private String remotePath(String appName, String volumeName) {
        return REMOTE + ":" + cfg.s3().bucket() + "/" + appName + "/" + volumeName;
    }

    private void sync(String appName, String volumeName, String src, String dst, String op) {
        if (!enabled()) {
            return;
        }
        String volName = "d4y_" + appName + "_" + volumeName;
        try {
            ensureRcloneImage();
            Map<String, Object> body = helperBody(volName, List.of("sync", src, dst));
            DockerHttpClient.Response created = docker.post(
                    "/containers/create?name=" + enc("d4y_backup_" + volName + "_" + op.toLowerCase()), toJson(body));
            if (!created.isSuccess()) {
                // evtl. Rest eines vorherigen Laufs: best effort weiter
                throw new DockerApiException(op + " (Helfer erstellen)", created.status(), created.body());
            }
            String id = readTree(created.body()).path("Id").asText();
            try {
                DockerHttpClient.Response started = docker.post("/containers/" + id + "/start", null);
                if (started.status() != 204 && started.status() != 304) {
                    throw new DockerApiException(op + " (Helfer starten)", started.status(), started.body());
                }
                DockerHttpClient.Response waited = docker.post("/containers/" + id + "/wait", null);
                int code = readTree(waited.body()).path("StatusCode").asInt(-1);
                if (code == 0) {
                    log.info("{} für Volume {} abgeschlossen", op, volName);
                } else {
                    log.warn("{} für Volume {} fehlgeschlagen (Exit {})", op, volName, code);
                }
            } finally {
                docker.delete("/containers/" + id + "?force=true");
            }
        } catch (RuntimeException e) {
            log.warn("{} für Volume {} nicht möglich: {}", op, volName, e.getMessage());
            log.debug("Details", e);
        }
    }

    private void ensureRcloneImage() {
        DockerHttpClient.Response res = docker.post(
                "/images/create?fromImage=" + RCLONE_REPO + "&tag=" + RCLONE_TAG, null);
        if (!res.isSuccess()) {
            throw new DockerApiException("rclone-Image beziehen", res.status(), res.body());
        }
    }

    private Map<String, Object> helperBody(String volName, List<String> cmd) {
        D4yProperties.S3 s3 = cfg.s3();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Image", RCLONE_REPO + ":" + RCLONE_TAG);
        body.put("Cmd", cmd);
        body.put("Labels", Map.of("d4y.system", "backup"));
        // rclone-Remote "store" komplett über Env (Credentials nicht in der Kommandozeile).
        List<String> env = new ArrayList<>();
        env.add("RCLONE_CONFIG_" + REMOTE.toUpperCase() + "_TYPE=s3");
        env.add("RCLONE_CONFIG_" + REMOTE.toUpperCase() + "_PROVIDER=" + s3.provider());
        env.add("RCLONE_CONFIG_" + REMOTE.toUpperCase() + "_ENDPOINT=" + s3.endpoint());
        env.add("RCLONE_CONFIG_" + REMOTE.toUpperCase() + "_REGION=" + s3.region());
        env.add("RCLONE_CONFIG_" + REMOTE.toUpperCase() + "_ACCESS_KEY_ID=" + s3.accessKey());
        env.add("RCLONE_CONFIG_" + REMOTE.toUpperCase() + "_SECRET_ACCESS_KEY=" + s3.secretKey());
        env.add("RCLONE_CONFIG_" + REMOTE.toUpperCase() + "_FORCE_PATH_STYLE=true");
        body.put("Env", env);
        Map<String, Object> hostConfig = new LinkedHashMap<>();
        hostConfig.put("NetworkMode", DockerEdgeProxy.NETWORK);
        hostConfig.put("Mounts", List.of(
                Map.of("Type", "volume", "Source", volName, "Target", "/data")));
        body.put("HostConfig", hostConfig);
        return body;
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String toJson(Object body) {
        try {
            return json.writeValueAsString(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JsonNode readTree(String body) {
        try {
            return json.readTree(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
