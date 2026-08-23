package com.sudolife.application.model.strava;

import lombok.Getter;

import java.time.Instant;

@Getter
public class StravaDataConsentRecord {

    private final Long id;
    private final String userEmail;
    private final StravaDataConsentPurpose purpose;
    private final String consentVersion;
    private final String language;
    private final Instant consentedAt;
    private final StravaDataConsentSource source;

    public StravaDataConsentRecord(Long id, String userEmail, StravaDataConsentPurpose purpose, String consentVersion,
                                   String language, Instant consentedAt, StravaDataConsentSource source) {
        validateText(userEmail, "User email is invalid");
        validateText(consentVersion, "Consent version is invalid");
        validateText(language, "Consent language is invalid");
        validateObject(purpose, "Consent purpose is invalid");
        validateObject(consentedAt, "Consented at is invalid");
        validateObject(source, "Consent source is invalid");

        this.id = id;
        this.userEmail = userEmail;
        this.purpose = purpose;
        this.consentVersion = consentVersion;
        this.language = language;
        this.consentedAt = consentedAt;
        this.source = source;
    }

    public static StravaDataConsentRecord accepted(String userEmail, String language, Instant consentedAt) {
        return new StravaDataConsentRecord(null, userEmail, StravaDataConsentPurpose.STRAVA_DATA_IMPORT_AND_COACHING,
                StravaDataConsentVersions.CURRENT, language, consentedAt, StravaDataConsentSource.STRAVA_CONNECTION);
    }

    private void validateText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateObject(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
