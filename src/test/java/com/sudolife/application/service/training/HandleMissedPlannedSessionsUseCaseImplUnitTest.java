package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.ports.provided.AdaptNextPlannedSessionUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleMissedPlannedSessionsUseCaseImplUnitTest {

    @Mock
    private AdaptiveRunningPlanRepository planRepository;

    @Mock
    private AdaptNextPlannedSessionUseCase adaptNextPlannedSessionUseCase;

    @Test
    void execute_before_grace_period_passes_keeps_session_planned() {
        AdaptiveRunningPlan plan = plan(PlannedSessionStatus.PLANNED, null);
        HandleMissedPlannedSessionsUseCaseImpl useCase = useCase("2026-08-05T00:00:00Z");
        when(planRepository.findLatestPlans()).thenReturn(List.of(plan));

        useCase.execute();

        assertThat(plan.getPlannedSessions().getFirst().getStatus()).isEqualTo(PlannedSessionStatus.PLANNED);
        verify(planRepository, never()).save(plan);
    }

    @Test
    void execute_after_grace_period_passes_marks_unmatched_session_missed_and_adapts_next_session() {
        AdaptiveRunningPlan plan = plan(PlannedSessionStatus.PLANNED, null);
        HandleMissedPlannedSessionsUseCaseImpl useCase = useCase("2026-08-05T00:00:01Z");
        when(planRepository.findLatestPlans()).thenReturn(List.of(plan));

        useCase.execute();

        assertThat(plan.getPlannedSessions().getFirst().getStatus()).isEqualTo(PlannedSessionStatus.MISSED);
        verify(planRepository).save(plan);
        verify(adaptNextPlannedSessionUseCase).execute(
                "runner@sudolife.com",
                new AdaptNextPlannedSessionCommand(AdaptationTrigger.MISSED_PLANNED_SESSION));
    }

    @Test
    void execute_after_grace_period_passes_keeps_late_matched_session_completed() {
        AdaptiveRunningPlan plan = plan(PlannedSessionStatus.COMPLETED, 10L);
        HandleMissedPlannedSessionsUseCaseImpl useCase = useCase("2026-08-05T00:00:01Z");
        when(planRepository.findLatestPlans()).thenReturn(List.of(plan));

        useCase.execute();

        assertThat(plan.getPlannedSessions().getFirst().getStatus()).isEqualTo(PlannedSessionStatus.COMPLETED);
        verify(planRepository, never()).save(plan);
        verify(adaptNextPlannedSessionUseCase, never()).execute(
                "runner@sudolife.com",
                new AdaptNextPlannedSessionCommand(AdaptationTrigger.MISSED_PLANNED_SESSION));
    }

    private HandleMissedPlannedSessionsUseCaseImpl useCase(String now) {
        TimeProvider timeProvider = () -> Instant.parse(now);

        return new HandleMissedPlannedSessionsUseCaseImpl(
                planRepository, adaptNextPlannedSessionUseCase, timeProvider, Duration.ofHours(24));
    }

    private AdaptiveRunningPlan plan(PlannedSessionStatus status, Long matchedActivityId) {
        PlannedSessionResult expired = session(1, "2026-08-03");
        PlannedSessionResult upcoming = session(2, "2026-08-10");

        return new AdaptiveRunningPlan(1L, "runner@sudolife.com",
                new RunningGoal(10.0, 360, LocalDate.parse("2026-10-01")), "Explanation",
                Instant.parse("2026-07-27T12:00:00Z"), List.of(
                new AdaptiveRunningPlanSession(2L, null, expired, status, null, matchedActivityId),
                new AdaptiveRunningPlanSession(3L, null, upcoming, PlannedSessionStatus.PLANNED)));
    }

    private PlannedSessionResult session(int number, String date) {
        return new PlannedSessionResult(1, number, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse(date), 1800);
    }
}
