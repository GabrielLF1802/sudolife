package com.sudolife.adapter.driven.persistence.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.PlannedSessionStatus;
import com.sudolife.application.service.training.PlannedSessionTargetResult;
import com.sudolife.application.service.training.PlannedSessionType;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@Transactional
class AdaptiveRunningPlanPersistenceAdapterIntegrationTest {

    @Autowired
    private AdaptiveRunningPlanRepository repository;

    @Test
    void save_persists_the_accepted_plan_and_explanation_as_history() {
        AdaptiveRunningPlan olderPlan = repository.save(plan(
                "Original accepted explanation",
                Instant.parse("2026-07-20T12:00:00Z")));
        AdaptiveRunningPlan newerPlan = repository.save(plan(
                "Current accepted explanation",
                Instant.parse("2026-07-27T12:00:00Z")));

        AdaptiveRunningPlan result = repository.findLatestByUserEmail("runner@sudolife.com").orElseThrow();

        assertThat(result.getId()).isEqualTo(newerPlan.getId());
        assertThat(result.getId()).isNotEqualTo(olderPlan.getId());
        assertThat(result.getExplanation()).isEqualTo("Current accepted explanation");
        assertThat(result.getPlannedSessions()).singleElement().satisfies(session ->
                assertThat(session.getPlannedSession()).isEqualTo(originalSession()));
    }

    @Test
    void save_persists_status_transitions_and_replacement_linkage() {
        AdaptiveRunningPlan persistedPlan = repository.save(plan(
                "Accepted explanation",
                Instant.parse("2026-07-27T12:00:00Z")));
        Long originalSessionId = persistedPlan.getPlannedSessions().getFirst().getId();
        persistedPlan.replacePlannedSession(originalSessionId, replacementSession());

        repository.save(persistedPlan);
        AdaptiveRunningPlan result = repository.findLatestByUserEmail("runner@sudolife.com").orElseThrow();

        assertThat(result.getPlannedSessions()).hasSize(2);
        assertThat(result.getPlannedSessions()).anySatisfy(session -> {
            assertThat(session.getId()).isEqualTo(originalSessionId);
            assertThat(session.getStatus()).isEqualTo(PlannedSessionStatus.REPLACED);
        });
        assertThat(result.getPlannedSessions()).anySatisfy(session -> {
            assertThat(session.getOriginalPlannedSessionId()).isEqualTo(originalSessionId);
            assertThat(session.getStatus()).isEqualTo(PlannedSessionStatus.PLANNED);
            assertThat(session.getPlannedSession()).isEqualTo(replacementSession());
        });
    }

    @Test
    void save_persists_planned_duration_and_matched_activity() {
        AdaptiveRunningPlan persistedPlan = repository.save(plan(
                "Accepted explanation",
                Instant.parse("2026-07-27T12:00:00Z")));
        AdaptiveRunningPlanSession session = persistedPlan.getPlannedSessions().getFirst();
        session.match(99L);

        repository.save(persistedPlan);
        AdaptiveRunningPlan result = repository.findLatestByUserEmail("runner@sudolife.com").orElseThrow();

        assertThat(result.getPlannedSessions()).singleElement().satisfies(savedSession -> {
            assertThat(savedSession.getMatchedActivityId()).isEqualTo(99L);
            assertThat(savedSession.getStatus()).isEqualTo(PlannedSessionStatus.COMPLETED);
            assertThat(savedSession.getPlannedSession().durationSeconds()).isEqualTo(1800);
        });
    }

    @Test
    void save_persists_post_session_perceived_effort() {
        AdaptiveRunningPlan persistedPlan = repository.save(plan(
                "Accepted explanation", Instant.parse("2026-07-27T12:00:00Z")));
        AdaptiveRunningPlanSession session = persistedPlan.getPlannedSessions().getFirst();
        session.match(99L);
        session.reportPostSessionPerceivedEffort(8);

        repository.save(persistedPlan);
        AdaptiveRunningPlan result = repository.findLatestByUserEmail("runner@sudolife.com").orElseThrow();

        assertThat(result.getPlannedSessions()).singleElement()
                .extracting(AdaptiveRunningPlanSession::getPostSessionPerceivedEffort)
                .isEqualTo(8);
    }

    private AdaptiveRunningPlan plan(String explanation, Instant acceptedAt) {
        return new AdaptiveRunningPlan(
                null,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                explanation,
                acceptedAt,
                List.of(AdaptiveRunningPlanSession.planned(originalSession()))
        );
    }

    private PlannedSessionResult originalSession() {
        return new PlannedSessionResult(
                1,
                1,
                PlannedSessionType.EASY_RUN,
                5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse("2026-08-03"),
                1800
        );
    }

    private PlannedSessionResult replacementSession() {
        return new PlannedSessionResult(
                1,
                1,
                PlannedSessionType.RECOVERY,
                3.0,
                PlannedSessionTargetResult.perceivedEffort(1, 3),
                LocalDate.parse("2026-08-03")
        );
    }
}
