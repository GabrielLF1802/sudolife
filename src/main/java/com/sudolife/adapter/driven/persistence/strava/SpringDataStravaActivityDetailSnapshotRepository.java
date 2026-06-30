package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivityDetailSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataStravaActivityDetailSnapshotRepository
        extends JpaRepository<StravaActivityDetailSnapshotEntity, Long> {

    Optional<StravaActivityDetailSnapshotEntity> findByActivitySummaryId(Long activitySummaryId);

    boolean existsByActivitySummaryId(Long activitySummaryId);
}
