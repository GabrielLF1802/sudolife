package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.training.ports.provided.AdaptNextPlannedSessionUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import com.sudolife.helper.FixedTimeProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
        "spring.task.scheduling.enabled=false"
})
@Import(FixedTimeProvider.class)
@Transactional
class AdaptNextPlannedSessionUseCaseImplIntegrationTest {

    @Autowired
    private AdaptNextPlannedSessionUseCase useCase;

    @Autowired
    private AdaptiveRunningPlanRepository repository;

    @ParameterizedTest
    @EnumSource(AdaptationTrigger.class)
    void execute_with_each_trigger_persists_the_replacement_and_keeps_later_sessions_stable(
            AdaptationTrigger trigger
    ) {
        AdaptiveRunningPlan savedPlan = repository.save(plan());
        PlannedSessionResult laterSession = savedPlan.getPlannedSessions().get(1).getPlannedSession();

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com",
                new AdaptNextPlannedSessionCommand(trigger));

        assertThat(result.plannedSessions()).hasSize(3);
        assertThat(result.plannedSessions()).anySatisfy(session -> {
            assertThat(session.status()).isEqualTo(PlannedSessionStatus.REPLACED);
            assertThat(session.plannedSession().sessionNumber()).isEqualTo(1);
        });
        assertThat(result.plannedSessions()).anySatisfy(session -> {
            assertThat(session.adaptationTrigger()).isEqualTo(trigger);
            assertThat(session.originalPlannedSessionId()).isNotNull();
        });
        assertThat(result.plannedSessions()).anySatisfy(session -> {
            assertThat(session.plannedSession()).isEqualTo(laterSession);
            assertThat(session.status()).isEqualTo(PlannedSessionStatus.PLANNED);
            assertThat(session.adaptationTrigger()).isNull();
        });
    }

    private AdaptiveRunningPlan plan() {
        return new AdaptiveRunningPlan(
                null,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                "Accepted explanation",
                Instant.parse("2026-07-20T12:00:00Z"),
                List.of(
                        AdaptiveRunningPlanSession.planned(session(1, 1, 5.0, "2026-07-29")),
                        AdaptiveRunningPlanSession.planned(session(1, 2, 7.0, "2026-08-01"))));
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
