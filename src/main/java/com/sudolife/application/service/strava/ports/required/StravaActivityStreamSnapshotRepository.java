package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaActivityStreamSnapshot;

import java.time.Instant;
import java.util.Optional;

public interface StravaActivityStreamSnapshotRepository {

    StravaActivityStreamSnapshot saveIfAbsent(StravaActivityStreamSnapshot snapshot);

    Optional<StravaActivityStreamSnapshot> findByActivitySummaryId(Long activitySummaryId);

    long countByAccountLinkId(Long accountLinkId);

    Optional<Instant> findLatestFetchedAtByAccountLinkId(Long accountLinkId);
}
