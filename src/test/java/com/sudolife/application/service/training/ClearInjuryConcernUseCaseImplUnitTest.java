package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClearInjuryConcernUseCaseImplUnitTest {

    @Test
    void execute_with_active_injury_reassesses_history_and_returns_conservative_replacement() {
        CoachingProfileRepository profileRepository = profileRepository();
        AdaptiveRunningPlanRepository planRepository = planRepository(plan());
        GetRunningHistorySnapshotUseCase historyUseCase = mock(GetRunningHistorySnapshotUseCase.class);
        when(historyUseCase.execute("runner@sudolife.com")).thenReturn(history());
        ClearInjuryConcernUseCaseImpl useCase = useCase(profileRepository, planRepository, historyUseCase);

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com", new ClearInjuryConcernCommand("MODERATE"));

        assertThat(result.plannedSessions()).hasSize(3);
        assertThat(result.plannedSessions().get(0).status()).isEqualTo(PlannedSessionStatus.REPLACED);
        assertThat(result.plannedSessions().get(2).adaptationTrigger())
                .isEqualTo(AdaptationTrigger.INJURY_CONCERN_CLEARED);
        assertThat(result.plannedSessions().get(2).plannedSession().type()).isEqualTo(PlannedSessionType.EASY_RUN);
        assertThat(result.plannedSessions().get(2).plannedSession().distanceKilometers()).isEqualTo(3.0);
        assertThat(result.plannedSessions().get(2).plannedSession().target())
                .isEqualTo(PlannedSessionTargetResult.perceivedEffort(1, 3));
    }

    private ClearInjuryConcernUseCaseImpl useCase(
            CoachingProfileRepository profileRepository,
            AdaptiveRunningPlanRepository planRepository,
            GetRunningHistorySnapshotUseCase historyUseCase
    ) {
        TimeProvider timeProvider = () -> Instant.parse("2026-07-27T12:00:00Z");

        return new ClearInjuryConcernUseCaseImpl(
                profileRepository,
                planRepository,
                historyUseCase,
                timeProvider);
    }

    private CoachingProfileRepository profileRepository() {
        CoachingProfileRepository repository = mock(CoachingProfileRepository.class);
        CoachingProfile profile = new CoachingProfile(
                1L,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                UserReportedReadiness.HIGH,
                true);
        when(repository.findByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(profile));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        return repository;
    }

    private AdaptiveRunningPlanRepository planRepository(AdaptiveRunningPlan plan) {
        AdaptiveRunningPlanRepository repository = mock(AdaptiveRunningPlanRepository.class);
        when(repository.findLatestByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(plan));
        when(repository.save(plan)).thenReturn(plan);

        return repository;
    }

    private RunningHistorySnapshotResult history() {
        return new RunningHistorySnapshotResult(
                true,
                3,
                4,
                16.0,
                5760,
                Instant.parse("2026-07-25T12:00:00Z"),
                List.of(),
                1.0,
                6.0,
                360.0,
                RunningVolumeTrend.STABLE);
    }

    private AdaptiveRunningPlan plan() {
        return new AdaptiveRunningPlan(
                1L,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                "Accepted explanation",
                Instant.parse("2026-07-20T12:00:00Z"),
                List.of(
                        new AdaptiveRunningPlanSession(10L, null, nextSession(), PlannedSessionStatus.PLANNED),
                        new AdaptiveRunningPlanSession(11L, null, laterSession(), PlannedSessionStatus.PLANNED)));
    }

    private PlannedSessionResult nextSession() {
        return session(1, 1, 0, "2026-07-28");
    }

    private PlannedSessionResult laterSession() {
        return session(1, 2, 7.0, "2026-08-01");
    }

    private PlannedSessionResult session(int week, int number, double distance, String date) {
        return new PlannedSessionResult(
                week,
                number,
                PlannedSessionType.RECOVERY,
                distance,
                PlannedSessionTargetResult.perceivedEffort(1, 3),
                LocalDate.parse(date));
    }
}
