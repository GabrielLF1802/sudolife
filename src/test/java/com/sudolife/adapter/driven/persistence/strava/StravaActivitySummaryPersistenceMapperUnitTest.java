package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivitySummaryEntity;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import org.junit.jupiter.api.Test;

import static com.sudolife.helper.StravaTestHelper.ACTIVITY_START_DATE;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.stravaActivitySummary;
import static org.assertj.core.api.Assertions.assertThat;

class StravaActivitySummaryPersistenceMapperUnitTest {

    private final StravaActivitySummaryPersistenceMapper mapper = new StravaActivitySummaryPersistenceMapper();

    @Test
    void to_entity_maps_activity_summary_fields() {
        StravaActivitySummary summary = stravaActivitySummary();

        StravaActivitySummaryEntity entity = mapper.toEntity(summary);

        assertThat(entity.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(entity.getAccountLinkId()).isEqualTo(LINK_ID);
        assertThat(entity.getSourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
        assertThat(entity.getActivityType()).isEqualTo(StravaActivityType.RUN);
        assertThat(entity.getRawSportType()).isEqualTo("Run");
        assertThat(entity.getName()).isEqualTo("Morning Run");
        assertThat(entity.getStartDate()).isEqualTo(ACTIVITY_START_DATE);
        assertThat(entity.getPaceSecondsPerKilometer()).isEqualTo(300.0);
        assertThat(entity.getImportedAt()).isEqualTo(NOW);
    }

    @Test
    void to_domain_maps_activity_summary_fields() {
        StravaActivitySummaryEntity entity = mapper.toEntity(stravaActivitySummary());
        entity.setId(7L);

        StravaActivitySummary summary = mapper.toDomain(entity);

        assertThat(summary.getId()).isEqualTo(7L);
        assertThat(summary.getUserEmail()).isEqualTo(USER_EMAIL);
        assertThat(summary.getSourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
        assertThat(summary.getActivityType()).isEqualTo(StravaActivityType.RUN);
        assertThat(summary.getPaceSecondsPerKilometer()).isEqualTo(300.0);
    }
}
