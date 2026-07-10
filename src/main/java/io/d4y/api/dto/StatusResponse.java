package io.d4y.api.dto;

import java.util.List;

/**
 * Read-only Gesamtstatus der Plattform: Soll gegen Ist (ADR-0004).
 *
 * @param overall      {@code IN_SYNC} oder {@code DRIFT}
 * @param applications Status je deklarierter Application
 * @param undeclared   verwaltete Container ohne Deklaration (Drift)
 */
public record StatusResponse(String overall,
                             List<AppStatus> applications,
                             List<ExtraContainer> undeclared) {

    /**
     * @param serviceName stabiler interner Name für Service-Discovery ({@code <app>.<internal-domain>})
     * @param state   IN_SYNC | MISSING | OUTDATED | STOPPED
     * @param hold    aktiver Hold für diese App, oder {@code null}
     * @param volumes deklarierte Named Volumes der App (Soll)
     * @param routes  deklarierte Routes (externer Ingress, Soll)
     * @param envKeys Schlüssel der deklarierten Umgebungsvariablen (ohne Werte — Geheimnisschutz)
     * @param backup  {@code true}, wenn für die App ein Backup deklariert ist (ADR-0020)
     */
    public record AppStatus(String name,
                            String serviceName,
                            String desiredImage,
                            String state,
                            boolean running,
                            String containerId,
                            HoldInfo hold,
                            List<VolumeInfo> volumes,
                            List<RouteInfo> routes,
                            List<String> envKeys,
                            boolean backup) {
    }

    /** Aktiver Hold in der Statusanzeige. */
    public record HoldInfo(String type, long remainingSeconds) {
    }

    /**
     * Ein nicht deklarierter, aber von D4Y verwalteter Container (Drift).
     *
     * @param running {@code true}, wenn der Container läuft
     * @param volumes Named Volumes, mit denen der Container erzeugt wurde (Ist)
     */
    public record ExtraContainer(String appName,
                                 String image,
                                 String containerId,
                                 boolean running,
                                 List<VolumeInfo> volumes) {
    }

    /** Deklaration eines Named Volumes: Name + Mount-Pfad im Container. */
    public record VolumeInfo(String name, String path) {
    }

    /** Deklaration einer Route: Hostname (+ Pfad + Ziel-Port) für externen Ingress. */
    public record RouteInfo(String host, String path, int port) {
    }
}
