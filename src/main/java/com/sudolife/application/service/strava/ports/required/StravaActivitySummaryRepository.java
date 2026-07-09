package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.service.strava.activity.StravaActivitySummaryPage;

import java.util.Optional;

public interface StravaActivitySummaryRepository {

    boolean saveIfAbsent(StravaActivitySummary activitySummary);

    long countByUserEmail(String userEmail);

    long countByAccountLinkId(Long accountLinkId);

    long countStreamsReadyByAccountLinkId(Long accountLinkId);

    StravaActivitySummaryPage findByUserEmail(String userEmail, int page, int size);

    Optional<StravaActivitySummary> findByIdAndUserEmail(Long id, String userEmail);

    Optional<StravaActivitySummary> findByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId);
}
