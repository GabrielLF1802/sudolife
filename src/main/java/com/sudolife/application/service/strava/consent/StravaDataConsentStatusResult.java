package com.sudolife.application.service.strava.consent;

public record StravaDataConsentStatusResult(boolean valid, String currentConsentVersion, String purpose) {
}
