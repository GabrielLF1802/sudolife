package com.sudolife.application.service.strava;

import java.time.Instant;

public record StravaLinkStatusResult(boolean linked, Long athleteId, StravaPermissionState permissionState,
                                     StravaSummaryStatus activitySummaryStatus,
                                     StravaPerformanceDataStatus performanceDataStatus,
                                     Instant lastSummarySyncTime, Instant lastStreamEnrichmentTime,
                                     long importedActivityCount, long streamsReadyActivityCount,
                                     StravaActivitySyncFailureReason failureReason) {
}
