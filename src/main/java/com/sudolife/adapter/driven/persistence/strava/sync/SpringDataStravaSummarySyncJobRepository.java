package com.sudolife.adapter.driven.persistence.strava.sync;

import com.sudolife.adapter.driven.persistence.strava.sync.entitymodel.StravaSummarySyncJobEntity;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
