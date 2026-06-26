package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.service.strava.StravaActivitySummaryPage;

public interface StravaActivitySummaryRepository {

    boolean saveIfAbsent(StravaActivitySummary activitySummary);

    long countByUserEmail(String userEmail);

    StravaActivitySummaryPage findByUserEmail(String userEmail, int page, int size);
}
