package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivitySummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStravaActivitySummaryRepository extends JpaRepository<StravaActivitySummaryEntity, Long> {

    boolean existsByUserEmailAndSourceActivityId(String userEmail, Long sourceActivityId);

    long countByUserEmail(String userEmail);
}
