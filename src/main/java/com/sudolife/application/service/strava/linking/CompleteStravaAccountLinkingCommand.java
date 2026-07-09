package com.sudolife.application.service.strava.linking;

public record CompleteStravaAccountLinkingCommand(String state, String code, String scope, String error) {

    @Override
    public String toString() {
        return "CompleteStravaAccountLinkingCommand[state=%s, code=<redacted>, scope=%s, error=%s]"
                .formatted(state, scope, error);
    }
}
