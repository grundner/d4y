package io.d4y.app;

import io.d4y.adapter.compose.ComposeObserver;
import io.d4y.adapter.compose.Sidecar;
import io.d4y.domain.model.AppProject;
import io.d4y.domain.model.ComposeProject;
import io.d4y.port.AppProjectSource;
import io.d4y.port.VolumeBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sichert periodisch die Compose-Volumes laufender Apps mit Backup-Opt-in (ADR-0020/0029): die zu
 * sichernden Volumes stehen in der Sidecar {@code d4y.yaml} ({@code backup.volumes}). Inert, solange
 * kein Backup-Store konfiguriert ist.
 */
@Component
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final AppProjectSource projectSource;
    private final ComposeObserver observer;
    private final VolumeBackup volumeBackup;
    private final String prefix;

    public BackupService(AppProjectSource projectSource, ComposeObserver observer,
                         VolumeBackup volumeBackup,
                         @Value("${d4y.compose.project-prefix:d4y-}") String prefix) {
        this.projectSource = projectSource;
        this.observer = observer;
        this.volumeBackup = volumeBackup;
        this.prefix = prefix;
    }

    @Scheduled(fixedDelayString = "${d4y.backup.interval-ms:300000}")
    public void run() {
        if (!volumeBackup.enabled()) {
            return;
        }
        try {
            Set<String> runningProjects = observer.observe(prefix).stream()
                    .filter(ComposeProject::allRunning)
                    .map(ComposeProject::name)
                    .collect(Collectors.toSet());
            for (AppProject app : projectSource.load()) {
                if (!runningProjects.contains(app.projectName(prefix))) {
                    continue;
                }
                for (String volume : Sidecar.backupVolumes(app)) {
                    volumeBackup.backup(app.name(), volume);
                }
            }
        } catch (Exception e) {
            log.warn("Backup-Durchlauf fehlgeschlagen (erneuter Versuch beim nächsten Intervall): {}",
                    e.getMessage());
            log.debug("Details", e);
        }
    }
}
