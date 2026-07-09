package com.sudolife.application.service.strava.authorization;

public record StravaAuthorizationRequest(String state, String redirectUri, String scope) {
}
