package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivityStreamSyncJobEntity;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SpringDataStravaActivityStreamSyncJobRepository
        extends JpaRepository<StravaActivityStreamSyncJobEntity, Long> {

    boolean existsByOpenActivitySummaryId(Long openActivitySummaryId);

    List<StravaActivityStreamSyncJobEntity> findByStatusAndRunAfterLessThanEqualOrderByPriorityAscRunAfterAscCreatedAtAsc(
            StravaSummarySyncJobStatus status, Instant now, Pageable pageable);
}
