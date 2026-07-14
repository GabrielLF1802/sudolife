package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.training.ports.provided.EvaluateRunningGoalUseCase;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
@Transactional
@Import(FixedTimeProvider.class)
class EvaluateRunningGoalUseCaseImplIntegrationTest {

    @Autowired
    private EvaluateRunningGoalUseCase useCase;

    @Autowired
    private SaveCoachingProfileUseCase saveCoachingProfileUseCase;

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private StravaActivitySummaryRepository activityRepository;

    @Test
    void execute_with_reachable_goal_returns_the_persisted_goal_as_the_safe_milestone() {
        saveRunsInFourWeeks();
        saveCoachingProfileUseCase.execute(USER_EMAIL,
                new SaveCoachingProfileCommand(6.0, 360, LocalDate.parse("2026-09-15"), "MODERATE", false));

        RunningGoalAssessmentResult result = useCase.execute(USER_EMAIL);

        assertThat(result.realistic()).isTrue();
        assertThat(result.safeMilestone()).isEqualTo(result.longTermGoal());
    }

    @Test
    void execute_with_unreachable_goal_preserves_it_and_returns_a_safe_milestone() {
        saveRunsInFourWeeks();
        saveCoachingProfileUseCase.execute(USER_EMAIL,
                new SaveCoachingProfileCommand(42.2, 240, LocalDate.parse("2026-05-18"), "MODERATE", false));

        RunningGoalAssessmentResult result = useCase.execute(USER_EMAIL);

        assertThat(result.realistic()).isFalse();
        assertThat(result.reasons()).containsExactly(
                RunningGoalAssessmentReason.UNREALISTIC_DISTANCE,
                RunningGoalAssessmentReason.UNREALISTIC_PACE,
                RunningGoalAssessmentReason.UNREALISTIC_TARGET_DATE);
        assertThat(result.longTermGoal().targetDistanceKilometers()).isEqualTo(42.2);
        assertThat(result.safeMilestone().targetDistanceKilometers()).isLessThan(42.2);
    }

    private void saveRunsInFourWeeks() {
        StravaAccountLink link = accountLinkRepository.save(StravaAccountLink.active(
                null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, LINKED_AT));

        for (int week = 0; week < 4; week++) {
            activityRepository.saveIfAbsent(activity(link.getId(), (long) week + 1, week * 7 + 1));
        }
    }

    private StravaActivitySummary activity(Long linkId, Long sourceId, long daysAgo) {
        return StravaActivitySummary.imported(USER_EMAIL, linkId, sourceId, StravaActivityType.RUN, "Run", "Run",
                NOW.minusSeconds(daysAgo * 86400), 5000.0, 1800, 3.0, 10.0, 4.0,
                null, null, null, null, null, NOW);
    }
}
