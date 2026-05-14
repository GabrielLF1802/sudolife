package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAuthorizationStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStravaAuthorizationStateRepository extends JpaRepository<StravaAuthorizationStateEntity, String> {
}
