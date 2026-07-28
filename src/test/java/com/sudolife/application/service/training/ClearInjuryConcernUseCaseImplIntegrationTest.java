package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.training.ports.provided.ClearInjuryConcernUseCase;
import com.sudolife.application.service.training.ports.provided.SaveCoachingProfileUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
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
        "spring.task.scheduling.enabled=false"
})
@Import(FixedTimeProvider.class)
@Transactional
class ClearInjuryConcernUseCaseImplIntegrationTest {

    @Autowired
    private ClearInjuryConcernUseCase useCase;

    @Autowired
    private SaveCoachingProfileUseCase saveCoachingProfileUseCase;

    @Autowired
    private CoachingProfileRepository coachingProfileRepository;

    @Autowired
    private AdaptiveRunningPlanRepository adaptiveRunningPlanRepository;

    @Test
    void execute_clears_injury_and_persists_conservative_session_with_plan_history() {
        saveCoachingProfileUseCase.execute("runner@sudolife.com", injuryProfile());
        adaptiveRunningPlanRepository.save(plan());

        CurrentAdaptiveRunningPlanResult result = useCase.execute(
                "runner@sudolife.com", new ClearInjuryConcernCommand("MODERATE"));

        assertThat(coachingProfileRepository.findByUserEmail("runner@sudolife.com").orElseThrow()
                .isInjuryConcern()).isFalse();
        assertThat(result.plannedSessions()).hasSize(3);
        assertThat(result.plannedSessions()).anySatisfy(session -> {
            assertThat(session.status()).isEqualTo(PlannedSessionStatus.REPLACED);
            assertThat(session.plannedSession().distanceKilometers()).isZero();
        });
        assertThat(result.plannedSessions()).anySatisfy(session -> {
            assertThat(session.originalPlannedSessionId()).isNotNull();
            assertThat(session.adaptationTrigger()).isEqualTo(AdaptationTrigger.INJURY_CONCERN_CLEARED);
            assertThat(session.plannedSession().distanceKilometers()).isEqualTo(1.5);
            assertThat(session.plannedSession().target())
                    .isEqualTo(PlannedSessionTargetResult.perceivedEffort(1, 3));
        });
    }

    private SaveCoachingProfileCommand injuryProfile() {
        return new SaveCoachingProfileCommand(
                10.0,
                330,
                LocalDate.parse("2026-10-01"),
                "HIGH",
                true);
    }

    private AdaptiveRunningPlan plan() {
        return new AdaptiveRunningPlan(
                null,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                "Accepted explanation",
                Instant.parse("2026-05-10T12:00:00Z"),
                List.of(
                        AdaptiveRunningPlanSession.planned(session(1, 1, 0, "2026-05-12")),
                        AdaptiveRunningPlanSession.planned(session(1, 2, 7.0, "2026-05-16"))));
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
