package com.sudolife.application.service.strava.activity;

public record StravaActivitySyncResult(StravaActivitySyncStatus status, StravaActivitySyncFailureReason failureReason, int importedActivityCount, long totalActivityCount) {
}
