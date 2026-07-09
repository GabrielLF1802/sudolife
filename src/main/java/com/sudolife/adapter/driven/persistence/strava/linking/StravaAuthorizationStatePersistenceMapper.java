package com.sudolife.adapter.driven.persistence.strava.linking;

import com.sudolife.adapter.driven.persistence.strava.linking.entitymodel.StravaAuthorizationStateEntity;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import org.springframework.stereotype.Component;

@Component
public class StravaAuthorizationStatePersistenceMapper {

    public StravaAuthorizationStateEntity toEntity(StravaAuthorizationState domain) {
        StravaAuthorizationStateEntity entity = new StravaAuthorizationStateEntity();
        entity.setState(domain.getState());
        entity.setUserEmail(domain.getUserEmail());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setConsumedAt(domain.getConsumedAt());

        return entity;
    }

    public StravaAuthorizationState toDomain(StravaAuthorizationStateEntity entity) {
        return new StravaAuthorizationState(
                entity.getState(),
                entity.getUserEmail(),
                entity.getExpiresAt(),
                entity.getConsumedAt()
        );
    }
}
