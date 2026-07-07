package io.d4y.api.dto;

import io.d4y.domain.model.HoldType;

import java.util.List;
import java.util.Map;

/** Request-Bodies der operativen Endpunkte (ADR-0013). Felder sind optional. */
public final class OperationRequests {

    private OperationRequests() {
    }

    public record Stop(Long durationSeconds) {
    }

    public record Params(Map<String, String> env, Long durationSeconds) {
    }

    public record HoldReq(HoldType type, Long durationSeconds) {
    }

    public record Exec(List<String> cmd) {
    }

    public static long duration(Long value) {
        return value == null ? 0L : value;
    }
}
