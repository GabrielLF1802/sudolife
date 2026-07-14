package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluateRunningGoalUseCaseImplUnitTest {

    private static final String USER_EMAIL = "runner@sudolife.com";
    private static final Instant NOW = Instant.parse("2026-07-14T12:00:00Z");

    private final CoachingProfileRepository coachingProfileRepository = mock(CoachingProfileRepository.class);
    private final GetRunningHistorySnapshotUseCase runningHistoryUseCase = mock(GetRunningHistorySnapshotUseCase.class);
    private final TimeProvider timeProvider = () -> NOW;
    private final EvaluateRunningGoalUseCaseImpl useCase =
            new EvaluateRunningGoalUseCaseImpl(coachingProfileRepository, runningHistoryUseCase, timeProvider);

    @Test
    void execute_with_realistic_goal_returns_the_goal_as_the_safe_milestone() {
        stubProfile(new RunningGoal(7.0, 340, LocalDate.parse("2026-09-15")), UserReportedReadiness.MODERATE);
        stubHistory(24.0, 8400);

        RunningGoalAssessmentResult result = useCase.execute(USER_EMAIL);

        assertThat(result.realistic()).isTrue();
        assertThat(result.reasons()).isEmpty();
        assertThat(result.safeMilestone()).isEqualTo(result.longTermGoal());
    }

    @Test
    void execute_with_unrealistic_distance_preserves_the_goal_and_bounds_the_milestone_distance() {
        stubProfile(new RunningGoal(42.2, 360, null), UserReportedReadiness.MODERATE);
        stubHistory(20.0, 7200);

        RunningGoalAssessmentResult result = useCase.execute(USER_EMAIL);

        assertThat(result.realistic()).isFalse();
        assertThat(result.reasons()).containsExactly(RunningGoalAssessmentReason.UNREALISTIC_DISTANCE);
        assertThat(result.longTermGoal().targetDistanceKilometers()).isEqualTo(42.2);
        assertThat(result.safeMilestone().targetDistanceKilometers()).isEqualTo(7.3);
        assertThat(result.safeMilestone().targetDate()).isEqualTo(LocalDate.parse("2026-08-11"));
    }

    @Test
    void execute_with_unrealistic_pace_preserves_the_goal_and_returns_a_reachable_pace() {
        stubProfile(new RunningGoal(6.0, 240, null), UserReportedReadiness.MODERATE);
        stubHistory(20.0, 7200);

        RunningGoalAssessmentResult result = useCase.execute(USER_EMAIL);

        assertThat(result.reasons()).containsExactly(RunningGoalAssessmentReason.UNREALISTIC_PACE);
        assertThat(result.longTermGoal().targetPaceSecondsPerKilometer()).isEqualTo(240);
        assertThat(result.safeMilestone().targetPaceSecondsPerKilometer()).isEqualTo(332);
    }

    @Test
    void execute_with_unrealistic_date_preserves_the_date_and_moves_the_milestone_to_the_safety_window() {
        stubProfile(new RunningGoal(6.0, 360, LocalDate.parse("2026-07-21")), UserReportedReadiness.MODERATE);
        stubHistory(20.0, 7200);

        RunningGoalAssessmentResult result = useCase.execute(USER_EMAIL);

        assertThat(result.reasons()).containsExactly(RunningGoalAssessmentReason.UNREALISTIC_TARGET_DATE);
        assertThat(result.longTermGoal().targetDate()).isEqualTo(LocalDate.parse("2026-07-21"));
        assertThat(result.safeMilestone().targetDate()).isEqualTo(LocalDate.parse("2026-08-11"));
    }

    private void stubProfile(RunningGoal goal, UserReportedReadiness readiness) {
        when(coachingProfileRepository.findByUserEmail(USER_EMAIL))
                .thenReturn(Optional.of(new CoachingProfile(1L, USER_EMAIL, goal, readiness, false)));
    }

    private void stubHistory(double distanceKilometers, long movingTimeSeconds) {
        when(runningHistoryUseCase.execute(USER_EMAIL)).thenReturn(
                new RunningHistorySnapshotResult(true, 4, 4, distanceKilometers, movingTimeSeconds, NOW));
    }
}
