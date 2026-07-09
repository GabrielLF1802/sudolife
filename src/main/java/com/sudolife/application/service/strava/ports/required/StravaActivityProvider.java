package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaActivityDetailImport;
import com.sudolife.application.model.strava.StravaActivityStreamImport;
import com.sudolife.application.service.strava.activity.StravaActivitySummaryImport;
import com.sudolife.application.service.strava.exception.StravaActivityUnavailableException;

import java.time.Instant;
import java.util.List;

public interface StravaActivityProvider {

    List<StravaActivitySummaryImport> fetchActivitySummaries(String accessToken, Instant after, Instant before);

    StravaActivityDetailImport fetchActivityDetail(String accessToken, Long sourceActivityId);

    default StravaActivityStreamImport fetchActivityStreams(String accessToken, Long sourceActivityId) {
        throw new StravaActivityUnavailableException();
    }
}
