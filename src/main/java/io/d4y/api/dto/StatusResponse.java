package io.d4y.api.dto;

import java.util.List;

/**
 * Read-only Gesamtstatus der Plattform: Soll (Compose-App-Projekte) gegen Ist (ADR-0004/0029).
 *
 * @param overall      {@code IN_SYNC} oder {@code DRIFT}
 * @param applications Status je deklarierter App (Compose-Projekt)
 * @param undeclared   von d4y verwaltete Compose-Projekte ohne Deklaration (Drift)
 */
public record StatusResponse(String overall,
                             List<AppStatus> applications,
                             List<ExtraProject> undeclared) {

    /**
     * Status einer App (Compose-Projekt).
     *
     * @param state    RUNNING | PARTIAL | STOPPED | MISSING
     * @param hold     aktiver Hold für diese App, oder {@code null}
     * @param services Services/Container des Projekts (Ist)
     * @param routes   deklarierte Routes aus der Sidecar {@code d4y.yaml} (Soll)
     */
    public record AppStatus(String name,
                            String state,
                            HoldInfo hold,
                            List<ServiceStatus> services,
                            List<RouteInfo> routes) {
    }

    /** Ein Service/Container eines Projekts. */
    public record ServiceStatus(String name, String image, String state) {
    }

    /** Aktiver Hold in der Statusanzeige. */
    public record HoldInfo(String type, long remainingSeconds) {
    }

    /** Deklaration einer Route: Hostname (+ Pfad + Ziel-Port + TLS) für externen Ingress. */
    public record RouteInfo(String host, String path, int port, boolean tls) {
    }

    /** Ein nicht deklariertes, aber von d4y verwaltetes Compose-Projekt (Drift). */
    public record ExtraProject(String name, List<ServiceStatus> services) {
    }
}
