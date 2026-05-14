package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAuthorizationStateEntity;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import org.junit.jupiter.api.Test;

import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.consumedAuthorizationState;
import static org.assertj.core.api.Assertions.assertThat;

class StravaAuthorizationStatePersistenceMapperUnitTest {

    private final StravaAuthorizationStatePersistenceMapper mapper = new StravaAuthorizationStatePersistenceMapper();

    @Test
    void to_entity_maps_state_fields() {
        StravaAuthorizationState authorizationState = consumedAuthorizationState();

        StravaAuthorizationStateEntity entity = mapper.toEntity(authorizationState);

        assertThat(entity.getState()).isEqualTo(STATE);
        assertThat(entity.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(entity.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(entity.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void to_domain_maps_entity_fields() {
        StravaAuthorizationStateEntity entity = consumedEntity();

        StravaAuthorizationState authorizationState = mapper.toDomain(entity);

        assertThat(authorizationState.getState()).isEqualTo(STATE);
        assertThat(authorizationState.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(authorizationState.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(authorizationState.getConsumedAt()).isEqualTo(NOW);
    }

    private StravaAuthorizationStateEntity consumedEntity() {
        StravaAuthorizationStateEntity entity = new StravaAuthorizationStateEntity();
        entity.setState(STATE);
        entity.setUserEmail(USER_EMAIL);
        entity.setExpiresAt(EXPIRES_AT);
        entity.setConsumedAt(NOW);

        return entity;
    }
}
