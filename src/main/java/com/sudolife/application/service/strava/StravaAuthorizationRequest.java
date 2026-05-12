package com.sudolife.application.service.strava;

public record StravaAuthorizationRequest(String state, String redirectUri, String scope) {
}
