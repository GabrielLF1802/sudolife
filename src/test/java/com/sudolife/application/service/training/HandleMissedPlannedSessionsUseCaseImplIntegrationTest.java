package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.training.ports.provided.HandleMissedPlannedSessionsUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "adaptive-coaching.missed-session-scheduling-enabled=false"
})
@Import(FixedTimeProvider.class)
@Transactional
class HandleMissedPlannedSessionsUseCaseImplIntegrationTest {

    @Autowired
    private HandleMissedPlannedSessionsUseCase useCase;

    @Autowired
    private AdaptiveRunningPlanRepository planRepository;

    @Test
    void execute_with_expired_unmatched_session_persists_missed_status_and_adaptation() {
        AdaptiveRunningPlan savedPlan = planRepository.save(plan());
        Long expiredSessionId = savedPlan.getPlannedSessions().getFirst().getId();

        useCase.execute();

        AdaptiveRunningPlan currentPlan = planRepository.findLatestByUserEmail("runner@sudolife.com").orElseThrow();
        assertThat(currentPlan.findPlannedSession(expiredSessionId).getStatus()).isEqualTo(PlannedSessionStatus.MISSED);
        assertThat(currentPlan.getPlannedSessions()).anySatisfy(session -> {
            assertThat(session.getAdaptationTrigger()).isEqualTo(AdaptationTrigger.MISSED_PLANNED_SESSION);
            assertThat(session.getOriginalPlannedSessionId()).isNotNull();
        });
    }

    private AdaptiveRunningPlan plan() {
        return new AdaptiveRunningPlan(null, "runner@sudolife.com",
                new RunningGoal(10.0, 360, LocalDate.parse("2026-10-01")), "Explanation",
                Instant.parse("2026-05-01T12:00:00Z"), List.of(
                AdaptiveRunningPlanSession.planned(session(1, "2026-05-08")),
                AdaptiveRunningPlanSession.planned(session(2, "2026-05-12"))));
    }

    private PlannedSessionResult session(int number, String date) {
        return new PlannedSessionResult(1, number, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse(date), 1800);
    }
}
