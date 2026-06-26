package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static com.sudolife.helper.StravaTestHelper.ACTIVITY_START_DATE;
import static com.sudolife.helper.StravaTestHelper.LINK_ID;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.SOURCE_ACTIVITY_ID;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.stravaActivitySummary;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListStravaActivitiesUseCaseImplUnitTest {

    @Mock
    private StravaActivitySummaryRepository activitySummaryRepository;

    @Mock
    private StravaActivityListMapper mapper;

    @InjectMocks
    private ListStravaActivitiesUseCaseImpl useCase;

    @Test
    void execute_returns_activity_list_projection_for_authenticated_user() {
        ListStravaActivitiesCommand command = new ListStravaActivitiesCommand(USER_EMAIL, 1, 2);
        StravaActivitySummary activitySummary = stravaActivitySummary();
        StravaActivityListItemResult listItem = listItem(StravaActivityStreamStatus.PENDING);
        when(activitySummaryRepository.findByUserEmail(USER_EMAIL, 1, 2))
                .thenReturn(new StravaActivitySummaryPage(List.of(activitySummary), 1, 2, 3, 2));
        when(mapper.toResult(activitySummary)).thenReturn(listItem);

        StravaActivityListResult result = useCase.execute(command);

        StravaActivityListItemResult activity = result.activities().getFirst();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(activity.id()).isNull();
        assertThat(activity.sourceActivityId()).isEqualTo(SOURCE_ACTIVITY_ID);
        assertThat(activity.name()).isEqualTo("Morning Run");
        assertThat(activity.sportType()).isEqualTo(StravaActivityType.RUN);
        assertThat(activity.startDate()).isEqualTo(ACTIVITY_START_DATE);
        assertThat(activity.distanceMeters()).isEqualTo(5000.0);
        assertThat(activity.movingTimeSeconds()).isEqualTo(1500);
        assertThat(activity.averageSpeedMetersPerSecond()).isEqualTo(3.33);
        assertThat(activity.averagePaceSecondsPerKilometer()).isEqualTo(300.0);
        assertThat(activity.streamStatus()).isEqualTo(StravaActivityStreamStatus.PENDING);
    }

    @Test
    void command_rejects_invalid_pagination() {
        assertThatThrownBy(() -> new ListStravaActivitiesCommand(USER_EMAIL, -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page must be zero or greater");
        assertThatThrownBy(() -> new ListStravaActivitiesCommand(USER_EMAIL, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Size must be greater than zero");
    }

    private StravaActivityListItemResult listItem(StravaActivityStreamStatus streamStatus) {
        return new StravaActivityListItemResult(null, SOURCE_ACTIVITY_ID, "Morning Run", StravaActivityType.RUN,
                ACTIVITY_START_DATE, 5000.0, 1500, 3.33, 300.0, streamStatus);
    }
}
