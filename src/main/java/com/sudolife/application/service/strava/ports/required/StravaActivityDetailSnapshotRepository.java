package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaActivityDetailSnapshot;

import java.util.Optional;

public interface StravaActivityDetailSnapshotRepository {

    Optional<StravaActivityDetailSnapshot> findByActivitySummaryId(Long activitySummaryId);

    StravaActivityDetailSnapshot saveIfAbsent(StravaActivityDetailSnapshot snapshot);
}
