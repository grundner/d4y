package io.d4y.port;

/**
 * Sichert und stellt die Daten eines Named Volumes in einem/aus einem externen Backup-Store wieder
 * her (ADR-0020). Engine-spezifische Umsetzung im Adapter.
 */
public interface VolumeBackup {

    /** {@code true}, wenn ein Backup-Store konfiguriert ist. */
    boolean enabled();

    /** Sichert das Named Volume der App in den Store. */
    void backup(String appName, String volumeName);

    /** Stellt das Named Volume aus dem Store wieder her (nur für neue/leere Volumes gedacht). */
    void restore(String appName, String volumeName);
}
