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
     * @param state IN_SYNC | MISSING | OUTDATED | STOPPED
     */
    public record AppStatus(String name,
                            String desiredImage,
                            String state,
                            boolean running,
                            String containerId) {
    }

    public record ExtraContainer(String appName, String image, String containerId) {
    }
}
