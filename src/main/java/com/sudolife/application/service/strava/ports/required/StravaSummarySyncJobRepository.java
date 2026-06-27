package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaSummarySyncJob;

import java.time.Instant;
import java.util.Optional;

public interface StravaSummarySyncJobRepository {

    boolean enqueueIfAbsent(StravaSummarySyncJob job);

    boolean hasQueuedOrRunningJob(Long accountLinkId);

    Optional<StravaSummarySyncJob> findById(Long id);

    Optional<StravaSummarySyncJob> findLatestByAccountLinkId(Long accountLinkId);

    Optional<StravaSummarySyncJob> findLatestCompletedByAccountLinkId(Long accountLinkId);

    Optional<StravaSummarySyncJob> findNextRunnable(Instant now);

    StravaSummarySyncJob save(StravaSummarySyncJob job);
}
