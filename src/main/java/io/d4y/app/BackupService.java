package io.d4y.app;

import io.d4y.domain.model.Application;
import io.d4y.domain.model.ObservedContainer;
import io.d4y.domain.model.VolumeMapping;
import io.d4y.port.ContainerBackend;
import io.d4y.port.DesiredStateSource;
import io.d4y.port.VolumeBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sichert periodisch die Volumes aller laufenden Apps mit aktivem Backup (ADR-0020). Inert, solange
 * kein Backup-Store konfiguriert ist.
 */
@Component
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final DesiredStateSource desiredStateSource;
    private final ContainerBackend backend;
    private final VolumeBackup volumeBackup;

    public BackupService(DesiredStateSource desiredStateSource, ContainerBackend backend,
                         VolumeBackup volumeBackup) {
        this.desiredStateSource = desiredStateSource;
        this.backend = backend;
        this.volumeBackup = volumeBackup;
    }

    @Scheduled(fixedDelayString = "${d4y.backup.interval-ms:300000}")
    public void run() {
        if (!volumeBackup.enabled()) {
            return;
        }
        try {
            Set<String> running = backend.observe().stream()
                    .filter(ObservedContainer::running)
                    .map(ObservedContainer::appName)
                    .collect(Collectors.toSet());
            for (Application app : desiredStateSource.load().applications()) {
                if (!app.backup() || app.volumes().isEmpty() || !running.contains(app.name())) {
                    continue;
                }
                for (VolumeMapping v : app.volumes()) {
                    volumeBackup.backup(app.name(), v.name());
                }
            }
        } catch (Exception e) {
            log.warn("Backup-Durchlauf fehlgeschlagen (erneuter Versuch beim nächsten Intervall): {}",
                    e.getMessage());
            log.debug("Details", e);
        }
    }
}
