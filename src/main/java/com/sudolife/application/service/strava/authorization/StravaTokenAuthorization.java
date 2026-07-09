package com.sudolife.application.service.strava.authorization;

import java.time.Instant;

public record StravaTokenAuthorization(Long athleteId, String accessToken, String refreshToken, Instant expiresAt,
                                       String scope) {

    @Override
    public String toString() {
        return "StravaTokenAuthorization[athleteId=%s, accessToken=<redacted>, refreshToken=<redacted>, expiresAt=%s, scope=%s]"
                .formatted(athleteId, expiresAt, scope);
    }
}
