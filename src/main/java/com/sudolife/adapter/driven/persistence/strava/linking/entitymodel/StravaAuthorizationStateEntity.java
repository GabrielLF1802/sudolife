package com.sudolife.adapter.driven.persistence.strava.linking.entitymodel;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "strava_authorization_states",
        indexes = @Index(name = "ix_strava_authorization_states_user_email", columnList = "user_email")
)
@Getter
@Setter
public class StravaAuthorizationStateEntity {

    @Id
    private String state;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
}
