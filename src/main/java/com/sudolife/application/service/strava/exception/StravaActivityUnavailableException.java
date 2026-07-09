package com.sudolife.application.service.strava.exception;

import com.sudolife.application.service.strava.activity.StravaActivitySummaryImport;

import java.util.List;

public class StravaActivityUnavailableException extends RuntimeException {

    private final List<StravaActivitySummaryImport> partialSummaries;

    public StravaActivityUnavailableException() {
        this(List.of(), null);
    }

    public StravaActivityUnavailableException(Throwable cause) {
        this(List.of(), cause);
    }

    public StravaActivityUnavailableException(List<StravaActivitySummaryImport> partialSummaries) {
        this(partialSummaries, null);
    }

    public StravaActivityUnavailableException(List<StravaActivitySummaryImport> partialSummaries, Throwable cause) {
        super(cause);
        this.partialSummaries = List.copyOf(partialSummaries);
    }

    public List<StravaActivitySummaryImport> partialSummaries() {
        return partialSummaries;
    }
}
