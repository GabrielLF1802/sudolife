package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaActivitySummary;

public interface StravaActivitySummaryRepository {

    boolean saveIfAbsent(StravaActivitySummary activitySummary);

    long countByUserEmail(String userEmail);
}
