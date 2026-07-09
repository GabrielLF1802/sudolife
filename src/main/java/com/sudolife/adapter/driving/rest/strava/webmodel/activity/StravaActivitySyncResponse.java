package com.sudolife.adapter.driving.rest.strava.webmodel.activity;

public record StravaActivitySyncResponse(String status, String failureReason, int importedActivityCount, long totalActivityCount) {
}
