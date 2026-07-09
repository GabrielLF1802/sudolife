package com.sudolife.adapter.driven.api.strava.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StravaTokenResponse(@JsonProperty("access_token") String accessToken,
                                  @JsonProperty("refresh_token") String refreshToken,
                                  @JsonProperty("expires_at") Long expiresAt,
                                  @JsonProperty("scope") String scope,
                                  @JsonProperty("athlete") StravaAthleteResponse athlete) {
}
