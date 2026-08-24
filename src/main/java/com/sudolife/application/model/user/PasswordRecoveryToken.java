package com.sudolife.application.model.user;

import lombok.Getter;

import java.time.Instant;

@Getter
public class PasswordRecoveryToken {

    private final Long id;
    private final String userEmail;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant usedAt;
    private final Instant createdAt;

    public PasswordRecoveryToken(Long id, String userEmail, String tokenHash, Instant expiresAt, Instant usedAt, Instant createdAt) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User email cant be null or empty");
        }

        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("Token hash cant be null or empty");
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException("Expiration cant be null");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Creation date cant be null");
        }

        this.id = id;
        this.userEmail = userEmail;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.createdAt = createdAt;
    }
}
