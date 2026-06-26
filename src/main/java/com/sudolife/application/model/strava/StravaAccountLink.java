package com.sudolife.application.model.strava;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;

@NoArgsConstructor
@Getter
public class StravaAccountLink {

    private static final String ACTIVITY_READ_SCOPE = "activity:read";
    private static final String DEFAULT_GRANTED_SCOPES = "read,activity:read";

    private Long id;
    private String userEmail;
    private Long athleteId;
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private String grantedScopes;
    private boolean linked;
    private Instant linkedAt;
    private Instant unlinkedAt;

    public StravaAccountLink(Long id, String userEmail, Long athleteId, String accessToken, String refreshToken,
                             Instant expiresAt, boolean linked, Instant linkedAt, Instant unlinkedAt) {
        this(id, userEmail, athleteId, accessToken, refreshToken, expiresAt, null, linked, linkedAt, unlinkedAt);
    }

    public StravaAccountLink(Long id, String userEmail, Long athleteId, String accessToken, String refreshToken,
                             Instant expiresAt, String grantedScopes, boolean linked, Instant linkedAt,
                             Instant unlinkedAt) {
        validateAuthorizationState(accessToken, refreshToken, expiresAt, linked, unlinkedAt);

        this.id = id;
        this.userEmail = userEmail;
        this.athleteId = athleteId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.grantedScopes = grantedScopes;
        this.linked = linked;
        this.linkedAt = linkedAt;
        this.unlinkedAt = unlinkedAt;
    }

    public static StravaAccountLink active(Long id, String userEmail, Long athleteId, String accessToken,
                                           String refreshToken, Instant expiresAt, Instant linkedAt) {
        return active(id, userEmail, athleteId, accessToken, refreshToken, expiresAt, DEFAULT_GRANTED_SCOPES, linkedAt);
    }

    public static StravaAccountLink active(Long id, String userEmail, Long athleteId, String accessToken,
                                           String refreshToken, Instant expiresAt, String grantedScopes,
                                           Instant linkedAt) {
        return new StravaAccountLink(id, userEmail, athleteId, accessToken, refreshToken, expiresAt, grantedScopes,
                true, linkedAt, null);
    }

    public boolean isInactive() {
        return !linked;
    }

    public boolean hasActivityReadScope() {
        return Arrays.stream(scopeValue().split("[,\\s]+"))
                .anyMatch(ACTIVITY_READ_SCOPE::equals);
    }

    public void deactivate(Instant unlinkedAt) {
        if (unlinkedAt == null) {
            throw new IllegalArgumentException("Unlinked at cant be null");
        }

        this.linked = false;
        this.unlinkedAt = unlinkedAt;
        this.accessToken = null;
        this.refreshToken = null;
        this.expiresAt = null;
        this.grantedScopes = null;
    }

    private void validateAuthorizationState(String accessToken, String refreshToken, Instant expiresAt, boolean active,
                                            Instant unlinkedAt) {
        if (active && unlinkedAt != null) {
            throw new IllegalArgumentException("Active link cant have unlinked at");
        }

        if (!active && unlinkedAt == null) {
            throw new IllegalArgumentException("Inactive link must have unlinked at");
        }

        if (!active && (accessToken != null || refreshToken != null || expiresAt != null)) {
            throw new IllegalArgumentException("Inactive link cant have authorization data");
        }
    }

    private String scopeValue() {
        if (grantedScopes == null) {
            return "";
        }

        return grantedScopes.trim();
    }
}
