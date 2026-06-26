package com.sudolife.adapter.driving.rest.strava.webmodel;

public record StravaActivitySyncResponse(String status, String failureReason, int importedActivityCount, long totalActivityCount) {
}
