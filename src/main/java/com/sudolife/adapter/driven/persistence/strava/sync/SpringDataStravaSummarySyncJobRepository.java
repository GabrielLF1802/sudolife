package com.sudolife.adapter.driven.persistence.strava.sync;

import com.sudolife.adapter.driven.persistence.strava.sync.entitymodel.StravaSummarySyncJobEntity;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpringDataStravaSummarySyncJobRepository extends JpaRepository<StravaSummarySyncJobEntity, Long> {

    boolean existsByOpenAccountLinkId(Long openAccountLinkId);

    List<StravaSummarySyncJobEntity> findByAccountLinkIdOrderByCreatedAtDescIdDesc(Long accountLinkId,
                                                                                   Pageable pageable);

    List<StravaSummarySyncJobEntity> findByAccountLinkIdAndStatusOrderByCompletedAtDescIdDesc(Long accountLinkId,
                                                                                              StravaSummarySyncJobStatus status,
                                                                                              Pageable pageable);

    List<StravaSummarySyncJobEntity> findByStatusAndRunAfterLessThanEqualOrderByRunAfterAscCreatedAtAsc(
            StravaSummarySyncJobStatus status, Instant now, Pageable pageable);

    void deleteByAccountLinkId(Long accountLinkId);

    @Modifying
    @Query("""
            update StravaSummarySyncJobEntity job
            set job.status = com.sudolife.application.model.strava.StravaSummarySyncJobStatus.CANCELLED,
                job.completedAt = :now,
                job.updatedAt = :now,
                job.openAccountLinkId = null
            where job.userEmail = :userEmail
            and job.status in (
                com.sudolife.application.model.strava.StravaSummarySyncJobStatus.QUEUED,
                com.sudolife.application.model.strava.StravaSummarySyncJobStatus.RUNNING
            )
            """)
    void cancelOpenByUserEmail(@Param("userEmail") String userEmail, @Param("now") Instant now);

    void deleteByUserEmail(String userEmail);
}
