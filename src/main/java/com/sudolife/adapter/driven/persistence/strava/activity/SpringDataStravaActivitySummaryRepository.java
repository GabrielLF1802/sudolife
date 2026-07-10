package com.sudolife.adapter.driven.persistence.strava.activity;

import com.sudolife.adapter.driven.persistence.strava.activity.entitymodel.StravaActivitySummaryEntity;
import com.sudolife.application.model.strava.StravaActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

public interface SpringDataStravaActivitySummaryRepository extends JpaRepository<StravaActivitySummaryEntity, Long> {

    boolean existsByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId);

    long countByUserEmail(String userEmail);

    long countByAccountLinkId(Long accountLinkId);

    long countByAccountLinkIdAndActivityType(Long accountLinkId, StravaActivityType activityType);

    Page<StravaActivitySummaryEntity> findByUserEmail(String userEmail, Pageable pageable);

    Optional<StravaActivitySummaryEntity> findByIdAndUserEmail(Long id, String userEmail);

    Optional<StravaActivitySummaryEntity> findByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId);

    List<StravaActivitySummaryEntity> findByUserEmailAndActivityTypeAndStartDateBetweenOrderByStartDateDesc(
            String userEmail, StravaActivityType activityType, Instant startDate, Instant endDate
    );

    void deleteByAccountLinkId(Long accountLinkId);
}
