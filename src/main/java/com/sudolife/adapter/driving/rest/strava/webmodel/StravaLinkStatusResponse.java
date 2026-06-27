package com.sudolife.adapter.driving.rest.strava.webmodel;

import java.time.Instant;

public record StravaLinkStatusResponse(boolean linked, Long athleteId, String permissionState,
                                       String activitySummaryStatus, String performanceDataStatus,
                                       Instant lastSummarySyncTime, Instant lastStreamEnrichmentTime,
                                       long importedActivityCount, long streamsReadyActivityCount,
                                       String failureReason) {
}
