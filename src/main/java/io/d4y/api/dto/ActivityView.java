package io.d4y.api.dto;

import io.d4y.domain.model.AuditEntry;

/** Read-Sicht auf einen Audit-Eintrag. */
public record ActivityView(
        String time,
        String actor,
        String app,
        String action,
        String result,
        boolean drift,
        String hold) {

    public static ActivityView of(AuditEntry e) {
        return new ActivityView(
                e.time().toString(), e.actor(), e.app(), e.action(), e.result(), e.drift(), e.holdInfo());
    }
}
