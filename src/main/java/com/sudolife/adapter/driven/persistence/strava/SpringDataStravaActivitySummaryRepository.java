package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivitySummaryEntity;
import com.sudolife.application.model.strava.StravaActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataStravaActivitySummaryRepository extends JpaRepository<StravaActivitySummaryEntity, Long> {

    boolean existsByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId);

    long countByUserEmail(String userEmail);

    long countByAccountLinkId(Long accountLinkId);

    long countByAccountLinkIdAndActivityType(Long accountLinkId, StravaActivityType activityType);

    Page<StravaActivitySummaryEntity> findByUserEmail(String userEmail, Pageable pageable);

    Optional<StravaActivitySummaryEntity> findByIdAndUserEmail(Long id, String userEmail);

    Optional<StravaActivitySummaryEntity> findByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId);
}
