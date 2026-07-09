package com.sudolife.adapter.driven.persistence.strava.activity;

import com.sudolife.adapter.driven.persistence.strava.activity.entitymodel.StravaActivityStreamSnapshotEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataStravaActivityStreamSnapshotRepository
        extends JpaRepository<StravaActivityStreamSnapshotEntity, Long> {

    Optional<StravaActivityStreamSnapshotEntity> findByActivitySummaryId(Long activitySummaryId);

    long countByAccountLinkId(Long accountLinkId);

    List<StravaActivityStreamSnapshotEntity> findByAccountLinkIdOrderByFetchedAtDescIdDesc(Long accountLinkId,
                                                                                           Pageable pageable);

    void deleteByAccountLinkId(Long accountLinkId);
}
