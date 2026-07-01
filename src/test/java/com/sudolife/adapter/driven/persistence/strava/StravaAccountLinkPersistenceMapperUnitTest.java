package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaAccountLinkEntity;
import com.sudolife.application.model.strava.StravaAccountLink;
import org.junit.jupiter.api.Test;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.UNLINKED_AT;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.activeStravaAccountLink;
import static com.sudolife.helper.StravaTestHelper.inactiveStravaAccountLink;
import static org.assertj.core.api.Assertions.assertThat;

class StravaAccountLinkPersistenceMapperUnitTest {

    private final StravaAccountLinkPersistenceMapper mapper = new StravaAccountLinkPersistenceMapper();

    @Test
    void to_entity_maps_active_link_fields() {
        StravaAccountLink link = activeStravaAccountLink();

        StravaAccountLinkEntity entity = mapper.toEntity(link);

        assertThat(entity.getId()).isEqualTo(LINK_ID);
        assertThat(entity.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(entity.getAthleteId()).isEqualTo(ATHLETE_ID);
        assertThat(entity.getActiveAthleteId()).isEqualTo(ATHLETE_ID);
        assertThat(entity.getActiveUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(entity.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(entity.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(entity.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(entity.getGrantedScopes()).isEqualTo(SCOPE);
        assertThat(entity.isActive()).isTrue();
        assertThat(entity.isReconnectRequired()).isFalse();
        assertThat(entity.getLinkedAt()).isEqualTo(LINKED_AT);
        assertThat(entity.getUnlinkedAt()).isNull();
    }

    @Test
    void to_entity_maps_inactive_link_without_authorization_metadata() {
        StravaAccountLink link = inactiveStravaAccountLink();

        StravaAccountLinkEntity entity = mapper.toEntity(link);

        assertThat(entity.getActiveAthleteId()).isNull();
        assertThat(entity.getActiveUserEmail()).isNull();
        assertThat(entity.getAccessToken()).isNull();
        assertThat(entity.getRefreshToken()).isNull();
        assertThat(entity.getExpiresAt()).isNull();
        assertThat(entity.getGrantedScopes()).isNull();
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.isReconnectRequired()).isFalse();
        assertThat(entity.getUnlinkedAt()).isEqualTo(UNLINKED_AT);
    }

    @Test
    void to_domain_maps_entity_fields() {
        StravaAccountLinkEntity entity = activeEntity();

        StravaAccountLink link = mapper.toDomain(entity);

        assertThat(link.getId()).isEqualTo(LINK_ID);
        assertThat(link.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(link.getAthleteId()).isEqualTo(ATHLETE_ID);
        assertThat(link.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(link.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(link.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(link.getGrantedScopes()).isEqualTo(SCOPE);
        assertThat(link.isLinked()).isTrue();
        assertThat(link.isReconnectRequired()).isFalse();
        assertThat(link.getLinkedAt()).isEqualTo(LINKED_AT);
        assertThat(link.getUnlinkedAt()).isNull();
    }

    @Test
    void to_entity_maps_reconnect_required_link_without_deactivating_it() {
        StravaAccountLink link = activeStravaAccountLink();
        link.markReconnectRequired();

        StravaAccountLinkEntity entity = mapper.toEntity(link);

        assertThat(entity.isActive()).isTrue();
        assertThat(entity.isReconnectRequired()).isTrue();
        assertThat(entity.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(entity.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    private StravaAccountLinkEntity activeEntity() {
        StravaAccountLinkEntity entity = new StravaAccountLinkEntity();
        entity.setId(LINK_ID);
        entity.setUserEmail(USER_EMAIL);
        entity.setAthleteId(ATHLETE_ID);
        entity.setActiveAthleteId(ATHLETE_ID);
        entity.setAccessToken(ACCESS_TOKEN);
        entity.setRefreshToken(REFRESH_TOKEN);
        entity.setExpiresAt(EXPIRES_AT);
        entity.setGrantedScopes(SCOPE);
        entity.setActive(true);
        entity.setReconnectRequired(false);
        entity.setLinkedAt(LINKED_AT);

        return entity;
    }
}
