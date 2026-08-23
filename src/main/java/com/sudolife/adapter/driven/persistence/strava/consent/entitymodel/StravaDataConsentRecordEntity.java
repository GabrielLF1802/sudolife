package com.sudolife.adapter.driven.persistence.strava.consent.entitymodel;

import com.sudolife.application.model.strava.StravaDataConsentPurpose;
import com.sudolife.application.model.strava.StravaDataConsentSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "strava_data_consent_records",
        indexes = @Index(name = "ix_strava_data_consent_records_user_email", columnList = "user_email"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_strava_data_consent_records_user_purpose_version",
                columnNames = {"user_email", "purpose", "consent_version"}
        )
)
@Getter
@Setter
public class StravaDataConsentRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StravaDataConsentPurpose purpose;
    @Column(name = "consent_version", nullable = false)
    private String consentVersion;
    @Column(nullable = false)
    private String language;
    @Column(name = "consented_at", nullable = false)
    private Instant consentedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StravaDataConsentSource source;
}
