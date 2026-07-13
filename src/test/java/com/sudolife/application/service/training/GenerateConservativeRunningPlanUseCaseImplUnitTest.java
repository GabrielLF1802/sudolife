package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerateConservativeRunningPlanUseCaseImplUnitTest {

    private static final String USER_EMAIL = "runner@sudolife.com";

    private final CoachingProfileRepository coachingProfileRepository = mock(CoachingProfileRepository.class);
    private final TrainingProfileRepository trainingProfileRepository = mock(TrainingProfileRepository.class);
    private final GetRunningHistorySnapshotUseCase runningHistoryUseCase = mock(GetRunningHistorySnapshotUseCase.class);
    private final GenerateConservativeRunningPlanUseCaseImpl useCase =
            new GenerateConservativeRunningPlanUseCaseImpl(
                    coachingProfileRepository, trainingProfileRepository, runningHistoryUseCase);

    @Test
    void execute_with_incomplete_history_returns_structured_conservative_plan_with_bounded_progression() {
        stubCoachingProfile(UserReportedReadiness.MODERATE);
        stubRunningHistory(false, 8.0, 2);
        when(trainingProfileRepository.findByUserEmail(USER_EMAIL))
                .thenReturn(Optional.of(new TrainingProfile(1L, USER_EMAIL, 1990)));

        ConservativeRunningPlanResult result = useCase.execute(USER_EMAIL);

        assertThat(result.classification()).isEqualTo(ConservativeRunningPlanClassification.CONSERVATIVE);
        assertThat(result.reasons()).containsExactly(ConservativeRunningPlanReason.INSUFFICIENT_HISTORY);
        assertThat(result.longTermGoalDistanceKilometers()).isEqualTo(21.1);
        assertThat(result.plannedSessions()).hasSize(8);
        assertThat(result.plannedSessions()).allSatisfy(session -> {
            assertThat(session.type()).isIn(PlannedSessionType.EASY_RUN, PlannedSessionType.LONG_RUN);
            assertThat(session.target().type()).isEqualTo(PlannedSessionTargetType.PERCEIVED_EFFORT);
            assertThat(session.target().maximumPerceivedEffort()).isLessThanOrEqualTo(4);
        });
        assertThat(maximumDistance(result, 4)).isLessThanOrEqualTo(maximumDistance(result, 1) * 1.16);
        assertThat(maximumDistance(result, 4)).isLessThan(result.longTermGoalDistanceKilometers());
    }

    @Test
    void execute_with_low_readiness_returns_conservative_plan_without_weekly_progression() {
        stubCoachingProfile(UserReportedReadiness.LOW);
        stubRunningHistory(true, 24.0, 4);
        when(trainingProfileRepository.findByUserEmail(USER_EMAIL))
                .thenReturn(Optional.of(new TrainingProfile(1L, USER_EMAIL, 1990)));

        ConservativeRunningPlanResult result = useCase.execute(USER_EMAIL);

        assertThat(result.reasons()).containsExactly(ConservativeRunningPlanReason.LOW_READINESS);
        assertThat(result.plannedSessions()).hasSize(8);
        assertThat(maximumDistance(result, 4)).isEqualTo(maximumDistance(result, 1));
        assertThat(result.plannedSessions()).allSatisfy(session ->
                assertThat(session.target().maximumPerceivedEffort()).isLessThanOrEqualTo(3));
    }

    @Test
    void execute_with_reliable_imported_zones_uses_low_heart_rate_guidance() {
        stubCoachingProfile(UserReportedReadiness.MODERATE);
        stubRunningHistory(false, 8.0, 2);
        TrainingProfile profile = new TrainingProfile(1L, USER_EMAIL, 1990,
                List.of(new TrainingHeartRateZone(100, 120), new TrainingHeartRateZone(121, 140)));
        when(trainingProfileRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.of(profile));

        ConservativeRunningPlanResult result = useCase.execute(USER_EMAIL);

        assertThat(result.plannedSessions()).allSatisfy(session -> {
            assertThat(session.target().type()).isEqualTo(PlannedSessionTargetType.HEART_RATE);
            assertThat(session.target().minimumHeartRate()).isEqualTo(100);
            assertThat(session.target().maximumHeartRate()).isEqualTo(140);
        });
    }

    private void stubCoachingProfile(UserReportedReadiness readiness) {
        RunningGoal runningGoal = new RunningGoal(21.1, 330, null);
        when(coachingProfileRepository.findByUserEmail(USER_EMAIL))
                .thenReturn(Optional.of(new CoachingProfile(1L, USER_EMAIL, runningGoal, readiness, false)));
    }

    private void stubRunningHistory(boolean sufficient, double totalDistanceKilometers, int activityCount) {
        when(runningHistoryUseCase.execute(USER_EMAIL)).thenReturn(new RunningHistorySnapshotResult(
                sufficient, sufficient ? 3 : 1, activityCount, totalDistanceKilometers, 3600, null));
    }

    private double maximumDistance(ConservativeRunningPlanResult result, int weekNumber) {
        return result.plannedSessions().stream()
                .filter(session -> session.weekNumber() == weekNumber)
                .mapToDouble(PlannedSessionResult::distanceKilometers)
                .max()
                .orElseThrow();
    }
}
