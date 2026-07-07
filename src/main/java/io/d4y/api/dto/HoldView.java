package io.d4y.api.dto;

import io.d4y.domain.model.Hold;

/** Read-Sicht auf einen aktiven Hold. */
public record HoldView(String app, String type, long remainingSeconds, String expiresAt) {

    public static HoldView of(Hold hold, long remainingSeconds) {
        return new HoldView(hold.appName(), hold.type().name(), remainingSeconds, hold.expiresAt().toString());
    }
}
