package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivitySummaryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStravaActivitySummaryRepository extends JpaRepository<StravaActivitySummaryEntity, Long> {

    boolean existsByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId);

    long countByUserEmail(String userEmail);

    Page<StravaActivitySummaryEntity> findByUserEmail(String userEmail, Pageable pageable);
}
