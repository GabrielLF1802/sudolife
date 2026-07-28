package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdaptNextPlannedSessionUseCaseImplUnitTest {

    @ParameterizedTest
    @EnumSource(AdaptationTrigger.class)
    void execute_with_each_trigger_replaces_only_the_next_planned_session(AdaptationTrigger trigger) {
        AdaptiveRunningPlanRepository repository = repositoryWith(plan());
        AdaptNextPlannedSessionUseCaseImpl useCase = useCase(repository);

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com", new AdaptNextPlannedSessionCommand(trigger));

        assertThat(result.plannedSessions()).hasSize(3);
        assertThat(result.plannedSessions().get(0).status()).isEqualTo(PlannedSessionStatus.REPLACED);
        assertThat(result.plannedSessions().get(1).plannedSession()).isEqualTo(laterSession());
        assertThat(result.plannedSessions().get(1).status()).isEqualTo(PlannedSessionStatus.PLANNED);
        assertThat(result.plannedSessions().get(2).originalPlannedSessionId()).isEqualTo(10L);
        assertThat(result.plannedSessions().get(2).adaptationTrigger()).isEqualTo(trigger);
    }

    private AdaptNextPlannedSessionUseCaseImpl useCase(AdaptiveRunningPlanRepository repository) {
        TimeProvider timeProvider = () -> Instant.parse("2026-07-27T12:00:00Z");

        return new AdaptNextPlannedSessionUseCaseImpl(
                repository, new AdaptedPlannedSessionValidator(), timeProvider);
    }

    private AdaptiveRunningPlanRepository repositoryWith(AdaptiveRunningPlan plan) {
        AdaptiveRunningPlanRepository repository = mock(AdaptiveRunningPlanRepository.class);
        when(repository.findLatestByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(plan));
        when(repository.save(plan)).thenReturn(plan);

        return repository;
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
        return session(1, 1, 5.0, "2026-07-28");
    }

    private PlannedSessionResult laterSession() {
        return session(1, 2, 7.0, "2026-08-01");
    }

    private PlannedSessionResult session(int week, int number, double distance, String date) {
        return new PlannedSessionResult(
                week,
                number,
                PlannedSessionType.EASY_RUN,
                distance,
                PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse(date));
    }
}
