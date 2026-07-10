package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.activity.StravaActivitySummaryPage;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

public interface StravaActivitySummaryRepository {

    boolean saveIfAbsent(StravaActivitySummary activitySummary);

    long countByUserEmail(String userEmail);

    long countByAccountLinkId(Long accountLinkId);

    long countStreamsReadyByAccountLinkId(Long accountLinkId);

    StravaActivitySummaryPage findByUserEmail(String userEmail, int page, int size);

    Optional<StravaActivitySummary> findByIdAndUserEmail(Long id, String userEmail);

    Optional<StravaActivitySummary> findByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId);

    List<StravaActivitySummary> findByUserEmailAndActivityTypeAndStartDateBetween(
            String userEmail, StravaActivityType activityType,
            Instant startDate, Instant endDate
    );
}
