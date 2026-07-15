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
        StravaAccountLink link = linkedAccount();
        activityRepository.saveIfAbsent(activity(link.getId(), 1L, StravaActivityType.RUN, 2));
        activityRepository.saveIfAbsent(activity(link.getId(), 2L, StravaActivityType.RUN, 9));
        activityRepository.saveIfAbsent(activity(link.getId(), 3L, StravaActivityType.RUN, 16));
        activityRepository.saveIfAbsent(activity(link.getId(), 5L, StravaActivityType.RUN, 58));
        activityRepository.saveIfAbsent(activity(link.getId(), 4L, StravaActivityType.RIDE, 23));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.sufficientRunningHistory()).isTrue();
        assertThat(result.activeWeeks()).isEqualTo(3);
        assertThat(result.runningActivityCount()).isEqualTo(4);
        assertThat(result.totalDistanceKilometers()).isEqualTo(20.0);
        assertThat(result.weeklyRunningVolumes()).hasSize(12);
    }

    @Test
    void execute_keeps_older_experience_without_classifying_recent_inactivity_as_sufficient() {
        StravaAccountLink link = linkedAccount();
        activityRepository.saveIfAbsent(activity(link.getId(), 1L, StravaActivityType.RUN, 3));
        activityRepository.saveIfAbsent(activity(link.getId(), 2L, StravaActivityType.RUN, 40));
        activityRepository.saveIfAbsent(activity(link.getId(), 3L, StravaActivityType.RUN, 61));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.sufficientRunningHistory()).isFalse();
        assertThat(result.activeWeeks()).isEqualTo(1);
        assertThat(result.runningActivityCount()).isEqualTo(3);
        assertThat(result.longestRunKilometers()).isEqualTo(5.0);
    }

    @Test
    void execute_produces_sparse_snapshot_when_optional_metrics_are_missing() {
        StravaAccountLink link = linkedAccount();
        activityRepository.saveIfAbsent(activity(link.getId(), 1L, StravaActivityType.RUN, 2, null, 1800));
        activityRepository.saveIfAbsent(activity(link.getId(), 2L, StravaActivityType.RUN, 37, 6000.0, null));

        RunningHistorySnapshotResult result = useCase.execute(USER_EMAIL);

        assertThat(result.sufficientRunningHistory()).isFalse();
        assertThat(result.runningActivityCount()).isEqualTo(2);
        assertThat(result.totalDistanceKilometers()).isEqualTo(6.0);
        assertThat(result.representativePaceSecondsPerKilometer()).isNull();
        assertThat(result.volumeTrend()).isEqualTo(RunningVolumeTrend.INSUFFICIENT_DATA);
    }

    private StravaActivitySummary activity(Long linkId, Long sourceId, StravaActivityType type, long daysAgo) {
        return activity(linkId, sourceId, type, daysAgo, 5000.0, 1800);
    }

    private StravaActivitySummary activity(
            Long linkId,
            Long sourceId,
            StravaActivityType type,
            long daysAgo,
            Double distanceMeters,
            Integer movingTimeSeconds
    ) {
        return StravaActivitySummary.imported(USER_EMAIL, linkId, sourceId, type, type.name(), type.name(),
                NOW.minusSeconds(daysAgo * 86400), distanceMeters, movingTimeSeconds, 3.0, 10.0, 4.0,
                null, null, null, null, null, NOW);
    }

    private StravaAccountLink linkedAccount() {
        return accountLinkRepository.save(StravaAccountLink.active(
                null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, LINKED_AT));
    }
}
