package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmitPostSessionPerceivedEffortUseCaseImplUnitTest {

    @Test
    void execute_for_completed_session_records_perceived_effort() {
        AdaptiveRunningPlan plan = completedPlan();
        AdaptiveRunningPlanRepository repository = repositoryWith(plan);
        SubmitPostSessionPerceivedEffortUseCaseImpl useCase = new SubmitPostSessionPerceivedEffortUseCaseImpl(repository);

        CurrentAdaptiveRunningPlanResult result = useCase.execute("runner@sudolife.com",
                new SubmitPostSessionPerceivedEffortCommand(2L, 8));

        assertThat(result.plannedSessions()).singleElement()
                .extracting(AdaptiveRunningPlanSessionResult::postSessionPerceivedEffort)
                .isEqualTo(8);
    }

    @Test
    void execute_for_incomplete_session_rejects_perceived_effort() {
        AdaptiveRunningPlan plan = plan(PlannedSessionStatus.PLANNED);
        SubmitPostSessionPerceivedEffortUseCaseImpl useCase =
                new SubmitPostSessionPerceivedEffortUseCaseImpl(repositoryWith(plan));

        assertThatThrownBy(() -> useCase.execute("runner@sudolife.com",
                new SubmitPostSessionPerceivedEffortCommand(2L, 8)))
                .isInstanceOf(IllegalStateException.class);
    }

    private AdaptiveRunningPlanRepository repositoryWith(AdaptiveRunningPlan plan) {
        AdaptiveRunningPlanRepository repository = mock(AdaptiveRunningPlanRepository.class);
        when(repository.findLatestByUserEmail("runner@sudolife.com")).thenReturn(Optional.of(plan));
        when(repository.save(plan)).thenReturn(plan);

        return repository;
    }

    private AdaptiveRunningPlan completedPlan() {
        return plan(PlannedSessionStatus.COMPLETED);
    }

    private AdaptiveRunningPlan plan(PlannedSessionStatus status) {
        PlannedSessionResult session = new PlannedSessionResult(1, 1, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse("2026-07-27"), 1800);

        return new AdaptiveRunningPlan(1L, "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")), "Explanation",
                Instant.parse("2026-07-20T12:00:00Z"),
                List.of(new AdaptiveRunningPlanSession(2L, null, session, status)));
    }
}
