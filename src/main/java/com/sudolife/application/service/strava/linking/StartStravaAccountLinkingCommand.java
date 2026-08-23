package com.sudolife.application.service.strava.linking;

public record StartStravaAccountLinkingCommand(String userEmail, boolean acceptedStravaDataConsent, String language) {

    public StartStravaAccountLinkingCommand(String userEmail) {
        this(userEmail, false, "pt-BR");
    }
}
