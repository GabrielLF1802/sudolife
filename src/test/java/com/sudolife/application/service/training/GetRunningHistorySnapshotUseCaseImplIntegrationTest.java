package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
@Transactional
@Import(FixedTimeProvider.class)
class GetRunningHistorySnapshotUseCaseImplIntegrationTest {

    @Autowired
    private GetRunningHistorySnapshotUseCase useCase;

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private StravaActivitySummaryRepository activityRepository;

    @Test
    void execute_calculates_sufficient_history_from_persisted_runs_and_ignores_other_activities() {
        StravaAccountLink link = accountLinkRepository.save(StravaAccountLink.active(
                null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, LINKED_AT));
        activityRepository.saveIfAbsent(activity(link.getId(), 1L, StravaActivityType.RUN, 2));
        activityRepository.saveIfAbsent(activity(link.getId(), 2L, StravaActivityType.RUN, 9));
        activityRepository.saveIfAbsent(activity(link.getId(), 3L, StravaActivityType.RUN, 16));
        activityRepository.saveIfAbsent(activity(link.getId(), 4L, StravaActivityType.RIDE, 23));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.sufficientRunningHistory()).isTrue();
        assertThat(result.activeWeeks()).isEqualTo(3);
        assertThat(result.runningActivityCount()).isEqualTo(3);
    }

    private StravaActivitySummary activity(Long linkId, Long sourceId, StravaActivityType type, long daysAgo) {
        return StravaActivitySummary.imported(USER_EMAIL, linkId, sourceId, type, type.name(), type.name(),
                NOW.minusSeconds(daysAgo * 86400), 5000.0, 1800, 3.0, 10.0, 4.0,
                null, null, null, null, null, NOW);
    }
}
