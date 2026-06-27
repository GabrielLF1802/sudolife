package com.sudolife.application.service.strava;

public enum StravaActivitySyncFailureReason {
    SYNC_ALREADY_RUNNING,
    PERMISSION_UPGRADE_REQUIRED,
    RECONNECT_REQUIRED,
    STRAVA_RATE_LIMITED,
    STRAVA_UNAVAILABLE,
    UNKNOWN_SYNC_FAILURE
}
