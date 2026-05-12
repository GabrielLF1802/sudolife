package com.sudolife.application.model.strava;

import java.time.Instant;

public class StravaAuthorizationState {

    private String state;
    private String userEmail;
    private Instant expiresAt;
    private Instant consumedAt;

    public StravaAuthorizationState(String state, String userEmail, Instant expiresAt, Instant consumedAt) {
        validateText(state, "State is invalid");
        validateText(userEmail, "User email is invalid");
        validateInstant(expiresAt, "Expires at cant be null");

        this.state = state;
        this.userEmail = userEmail;
        this.expiresAt = expiresAt;
        this.consumedAt = consumedAt;
    }

    public static StravaAuthorizationState pending(String state, String userEmail, Instant expiresAt) {
        return new StravaAuthorizationState(state, userEmail, expiresAt, null);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isPending() {
        return !isConsumed();
    }

    public boolean isExpiredAt(Instant now) {
        validateInstant(now, "Now cant be null");

        return !expiresAt.isAfter(now);
    }

    public void consume(Instant consumedAt) {
        validateInstant(consumedAt, "Consumed at cant be null");

        if (isConsumed()) {
            throw new IllegalStateException("Strava authorization state already consumed");
        }

        this.consumedAt = consumedAt;
    }

    public String getState() {
        return state;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    private void validateText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
