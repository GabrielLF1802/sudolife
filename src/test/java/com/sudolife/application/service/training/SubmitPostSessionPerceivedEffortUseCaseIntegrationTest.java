package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.training.ports.provided.SubmitPostSessionPerceivedEffortUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.task.scheduling.enabled=false"
})
@Transactional
class SubmitPostSessionPerceivedEffortUseCaseIntegrationTest {

    @Autowired
    private SubmitPostSessionPerceivedEffortUseCase useCase;

    @Autowired
    private AdaptiveRunningPlanRepository repository;

    @Test
    void execute_with_completed_session_persists_user_reported_effort() {
        Long sessionId = savePlan(PlannedSessionStatus.COMPLETED).getPlannedSessions().getFirst().getId();

        CurrentAdaptiveRunningPlanResult result = useCase.execute("runner@sudolife.com",
                new SubmitPostSessionPerceivedEffortCommand(sessionId, 8));

        assertThat(result.plannedSessions()).singleElement()
                .extracting(AdaptiveRunningPlanSessionResult::postSessionPerceivedEffort)
                .isEqualTo(8);
        assertThat(repository.findLatestByUserEmail("runner@sudolife.com").orElseThrow()
                .getPlannedSessions().getFirst().getPostSessionPerceivedEffort()).isEqualTo(8);
    }

    @Test
    void execute_with_incomplete_session_rejects_user_reported_effort() {
        Long sessionId = savePlan(PlannedSessionStatus.PLANNED).getPlannedSessions().getFirst().getId();

        var command = new SubmitPostSessionPerceivedEffortCommand(sessionId, 8);

        assertThatThrownBy(() -> useCase.execute("runner@sudolife.com", command))
                .isInstanceOf(IllegalStateException.class);
    }

    private AdaptiveRunningPlan savePlan(PlannedSessionStatus status) {
        PlannedSessionResult session = new PlannedSessionResult(1, 1, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse("2026-07-27"), 1800);
        AdaptiveRunningPlan plan = new AdaptiveRunningPlan(null, "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")), "Explanation",
                Instant.parse("2026-07-20T12:00:00Z"),
                List.of(new AdaptiveRunningPlanSession(null, null, session, status)));

        return repository.save(plan);
    }
}
