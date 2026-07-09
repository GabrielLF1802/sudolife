package com.sudolife.adapter.driving.rest.strava.webmodel.linking;

public record StravaCallbackRequest(String code, String scope, String state, String error) {
}
