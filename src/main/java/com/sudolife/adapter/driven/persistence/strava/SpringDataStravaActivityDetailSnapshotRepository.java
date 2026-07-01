package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivityDetailSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SpringDataStravaActivityDetailSnapshotRepository
        extends JpaRepository<StravaActivityDetailSnapshotEntity, Long> {

    Optional<StravaActivityDetailSnapshotEntity> findByActivitySummaryId(Long activitySummaryId);

    boolean existsByActivitySummaryId(Long activitySummaryId);

    @Modifying
    @Query("""
            delete from StravaActivityDetailSnapshotEntity snapshot
            where snapshot.activitySummaryId in (
                select summary.id
                from StravaActivitySummaryEntity summary
                where summary.accountLinkId = :accountLinkId
            )
            """)
    void deleteByAccountLinkId(Long accountLinkId);
}
