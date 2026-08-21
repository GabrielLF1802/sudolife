package com.sudolife.application.service.user;

import java.time.Instant;

public record StravaDeauthorization(Long athleteId, String accessToken, String refreshToken, Instant expiresAt) {

    public boolean hasRefreshToken() {
        return hasText(refreshToken);
    }

    public boolean hasUsableAccessToken(Instant now) {
        return hasText(accessToken) && expiresAt != null && expiresAt.isAfter(now);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
