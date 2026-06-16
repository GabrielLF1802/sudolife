package com.sudolife.adapter.driven.persistence.strava.entitymodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "strava_account_links",
        indexes = {
                @Index(name = "ix_strava_account_links_user_email_active", columnList = "user_email, active"),
                @Index(name = "ix_strava_account_links_athlete_id_active", columnList = "athlete_id, active")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_strava_account_links_active_athlete_id",
                        columnNames = "active_athlete_id"
                ),
                @UniqueConstraint(
                        name = "uk_strava_account_links_active_user_email",
                        columnNames = "active_user_email"
                )
        }
)
@Getter
@Setter
public class StravaAccountLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Column(name = "athlete_id", nullable = false)
    private Long athleteId;
    @Column(name = "active_athlete_id")
    private Long activeAthleteId;
    @Column(name = "active_user_email")
    private String activeUserEmail;
    @Column(name = "access_token", length = 2048)
    private String accessToken;
    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;
    @Column(name = "unlinked_at")
    private Instant unlinkedAt;
}
