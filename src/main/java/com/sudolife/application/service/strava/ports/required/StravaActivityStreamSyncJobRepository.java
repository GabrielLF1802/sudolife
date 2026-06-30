package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;

import java.time.Instant;
import java.util.Optional;

public interface StravaActivityStreamSyncJobRepository {

    boolean enqueueIfAbsent(StravaActivityStreamSyncJob job);

    Optional<StravaActivityStreamSyncJob> findById(Long id);

    Optional<StravaActivityStreamSyncJob> findNextRunnable(Instant now);

    StravaActivityStreamSyncJob save(StravaActivityStreamSyncJob job);
}
