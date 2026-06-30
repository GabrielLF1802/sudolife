package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.sudolife.helper.StravaTestHelper.ACTIVITY_START_DATE;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.stravaActivitySummary;
import static org.assertj.core.api.Assertions.assertThat;

class StravaActivityListMapperUnitTest {

    private final StravaActivityListMapper mapper = new StravaActivityListMapper();

    @Test
    void to_result_maps_activity_summary_fields() {
        StravaActivitySummary summary = stravaActivitySummary();

        StravaActivityListItemResult result = mapper.toResult(summary);

        assertThat(result.id()).isNull();
        assertThat(result.sourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
        assertThat(result.name()).isEqualTo("Morning Run");
        assertThat(result.sportType()).isEqualTo(StravaActivityType.RUN);
        assertThat(result.startDate()).isEqualTo(ACTIVITY_START_DATE);
        assertThat(result.distanceMeters()).isEqualTo(5000.0);
        assertThat(result.movingTimeSeconds()).isEqualTo(1500);
        assertThat(result.averageSpeedMetersPerSecond()).isEqualTo(3.33);
        assertThat(result.averagePaceSecondsPerKilometer()).isEqualTo(300.0);
        assertThat(result.streamStatus()).isEqualTo(StravaActivityStreamStatus.PENDING);
    }

    @Test
    void to_result_marks_weight_training_as_not_requiring_streams() {
        StravaActivitySummary summary = weightTrainingSummary();

        StravaActivityListItemResult result = mapper.toResult(summary);

        assertThat(result.streamStatus()).isEqualTo(StravaActivityStreamStatus.NOT_REQUIRED);
    }

    @Test
    void to_result_marks_eligible_activity_with_snapshot_as_completed() {
        StravaActivitySummary summary = stravaActivitySummary();

        StravaActivityListItemResult result = mapper.toResult(summary, true);

        assertThat(result.streamStatus()).isEqualTo(StravaActivityStreamStatus.COMPLETED);
    }

    private StravaActivitySummary weightTrainingSummary() {
        return StravaActivitySummary.imported(USER_EMAIL, LINK_ID, SOURCE_ACTIVITY_ID + 1,
                StravaActivityType.WEIGHT_TRAINING, "WeightTraining", "Strength",
                Instant.parse("2026-05-09T09:00:00Z"), null, 1800, null, null, null, null, null, null,
                null, 200.0, NOW);
    }
}
