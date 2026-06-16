package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAuthorizationStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface SpringDataStravaAuthorizationStateRepository extends JpaRepository<StravaAuthorizationStateEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StravaAuthorizationStateEntity authorizationState
            set authorizationState.consumedAt = :consumedAt
            where authorizationState.state = :state
            and authorizationState.consumedAt is null
            and authorizationState.expiresAt > :now
            """)
    int consumePending(@Param("state") String state, @Param("now") Instant now, @Param("consumedAt") Instant consumedAt);
}
