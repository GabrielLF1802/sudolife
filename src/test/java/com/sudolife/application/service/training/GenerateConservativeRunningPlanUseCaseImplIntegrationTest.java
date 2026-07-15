package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaAccountLink;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.service.strava.ports.required.StravaAccountLinkRepository;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.training.ports.provided.GenerateConservativeRunningPlanUseCase;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.LINKED_AT;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
@Transactional
@Import(FixedTimeProvider.class)
class GenerateConservativeRunningPlanUseCaseImplIntegrationTest {

    @Autowired
    private GenerateConservativeRunningPlanUseCase generateUseCase;

    @Autowired
    private SaveCoachingProfileUseCase saveCoachingProfileUseCase;

    @Autowired
    private StravaAccountLinkRepository accountLinkRepository;

    @Autowired
    private StravaActivitySummaryRepository activityRepository;

    @Test
    void execute_with_no_running_history_returns_persisted_profile_goal_and_conservative_sessions() {
        saveCoachingProfileUseCase.execute(USER_EMAIL, coachingCommand("MODERATE"));

        ConservativeRunningPlanResult result = generateUseCase.execute(USER_EMAIL);

        assertThat(result.classification()).isEqualTo(ConservativeRunningPlanClassification.CONSERVATIVE);
        assertThat(result.reasons()).containsExactly(ConservativeRunningPlanReason.INSUFFICIENT_HISTORY);
        assertThat(result.longTermGoalDistanceKilometers()).isEqualTo(21.1);
        assertThat(result.plannedSessions()).hasSize(8);
        assertThat(result.plannedSessions()).extracting(session -> session.scheduledDate().getDayOfWeek())
                .containsOnly(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY);
    }

    @Test
    void execute_with_persisted_running_availability_uses_preferred_days() {
        saveCoachingProfileUseCase.execute(USER_EMAIL,
                coachingCommand("MODERATE", false, List.of("WEDNESDAY", "SUNDAY")));

        ConservativeRunningPlanResult result = generateUseCase.execute(USER_EMAIL);

        assertThat(result.sessionsPerWeek()).isEqualTo(2);
        assertThat(result.plannedSessions()).extracting(session -> session.scheduledDate().getDayOfWeek())
                .containsOnly(DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void execute_with_unsafe_persisted_availability_reduces_weekly_sessions() {
        saveCoachingProfileUseCase.execute(USER_EMAIL,
                coachingCommand("MODERATE", false, List.of("MONDAY", "TUESDAY")));

        ConservativeRunningPlanResult result = generateUseCase.execute(USER_EMAIL);

        assertThat(result.sessionsPerWeek()).isEqualTo(1);
        assertThat(result.plannedSessions()).hasSize(4);
    }

    @Test
    void execute_with_sufficient_history_and_low_readiness_returns_non_progressing_conservative_sessions() {
        saveCoachingProfileUseCase.execute(USER_EMAIL, coachingCommand("LOW"));
        saveRunsInThreeWeeks();

        ConservativeRunningPlanResult result = generateUseCase.execute(USER_EMAIL);

        assertThat(result.reasons()).containsExactly(ConservativeRunningPlanReason.LOW_READINESS);
        assertThat(result.weeklyProgressionPercent()).isZero();
        assertThat(result.plannedSessions()).allSatisfy(session ->
                assertThat(session.target().maximumPerceivedEffort()).isEqualTo(3));
    }

    @Test
    void execute_with_injury_concern_returns_only_recovery_sessions_with_rpe_fallback() {
        saveCoachingProfileUseCase.execute(USER_EMAIL, coachingCommand("HIGH", true));
        saveRunsInThreeWeeks();

        ConservativeRunningPlanResult result = generateUseCase.execute(USER_EMAIL);

        assertThat(result.classification()).isEqualTo(ConservativeRunningPlanClassification.RECOVERY_ONLY);
        assertThat(result.reasons()).containsExactly(ConservativeRunningPlanReason.INJURY_CONCERN);
        assertThat(result.plannedSessions()).allSatisfy(session -> {
            assertThat(session.type()).isEqualTo(PlannedSessionType.RECOVERY);
            assertThat(session.target()).isEqualTo(PlannedSessionTargetResult.perceivedEffort(1, 3));
        });
    }

    @Test
    void execute_without_a_coaching_profile_rejects_the_request() {
        assertThatThrownBy(() -> generateUseCase.execute(USER_EMAIL))
                .hasMessage("A configured coaching profile is required");
    }

    private SaveCoachingProfileCommand coachingCommand(String readiness) {
        return coachingCommand(readiness, false);
    }

    private SaveCoachingProfileCommand coachingCommand(String readiness, boolean injuryConcern) {
        return coachingCommand(readiness, injuryConcern, List.of());
    }

    private SaveCoachingProfileCommand coachingCommand(
            String readiness,
            boolean injuryConcern,
            List<String> preferredRunningDays
    ) {
        return new SaveCoachingProfileCommand(21.1, 330, null, readiness, injuryConcern, preferredRunningDays);
    }

    private void saveRunsInThreeWeeks() {
        StravaAccountLink link = accountLinkRepository.save(StravaAccountLink.active(
                null, USER_EMAIL, ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT, LINKED_AT));
        activityRepository.saveIfAbsent(activity(link.getId(), 1L, 2));
        activityRepository.saveIfAbsent(activity(link.getId(), 2L, 9));
        activityRepository.saveIfAbsent(activity(link.getId(), 3L, 16));
    }

    private StravaActivitySummary activity(Long linkId, Long sourceId, long daysAgo) {
        return StravaActivitySummary.imported(USER_EMAIL, linkId, sourceId, StravaActivityType.RUN, "Run", "Run",
                NOW.minusSeconds(daysAgo * 86400), 5000.0, 1800, 3.0, 10.0, 4.0,
                null, null, null, null, null, NOW);
    }
}
