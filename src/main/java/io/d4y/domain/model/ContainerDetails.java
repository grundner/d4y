package io.d4y.domain.model;

import java.util.List;

/**
 * Ausgewählte Details eines Containers (Ergebnis von inspect).
 */
public record ContainerDetails(
        String id,
        String name,
        String image,
        String state,
        String status,
        String createdAt,
        List<String> env) {
}
