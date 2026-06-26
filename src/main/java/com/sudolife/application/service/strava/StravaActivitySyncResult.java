package com.sudolife.application.service.strava;

public record StravaActivitySyncResult(StravaActivitySyncStatus status, StravaActivitySyncFailureReason failureReason, int importedActivityCount, long totalActivityCount) {
}
