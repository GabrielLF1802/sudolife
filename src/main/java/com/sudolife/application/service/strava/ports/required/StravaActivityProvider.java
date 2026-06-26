package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.service.strava.StravaActivitySummaryImport;

import java.time.Instant;
import java.util.List;

public interface StravaActivityProvider {

    List<StravaActivitySummaryImport> fetchActivitySummaries(String accessToken, Instant after, Instant before);
}
