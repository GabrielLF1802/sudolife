package com.sudolife.application.service.strava.exception;

import com.sudolife.application.service.strava.activity.StravaActivitySummaryImport;

import java.util.List;

public class StravaActivityRateLimitException extends RuntimeException {

    private final List<StravaActivitySummaryImport> partialSummaries;

    public StravaActivityRateLimitException() {
        this(List.of());
    }

    public StravaActivityRateLimitException(List<StravaActivitySummaryImport> partialSummaries) {
        this.partialSummaries = List.copyOf(partialSummaries);
    }

    public List<StravaActivitySummaryImport> partialSummaries() {
        return partialSummaries;
    }
}
