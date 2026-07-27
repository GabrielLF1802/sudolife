package com.sudolife.application.model.training;

import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.PlannedSessionStatus;
import com.sudolife.application.service.training.PlannedSessionTargetResult;
import com.sudolife.application.service.training.PlannedSessionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdaptiveRunningPlanUnitTest {

    @Test
    void replace_planned_session_preserves_the_original_and_links_the_replacement() {
        AdaptiveRunningPlan plan = plan(persistedSession());

        AdaptiveRunningPlanSession replacement = plan.replacePlannedSession(10L, replacementSession());

        assertThat(plan.getPlannedSessions()).hasSize(2);
        assertThat(plan.getPlannedSessions().getFirst().getStatus()).isEqualTo(PlannedSessionStatus.REPLACED);
        assertThat(replacement.getOriginalPlannedSessionId()).isEqualTo(10L);
        assertThat(replacement.getStatus()).isEqualTo(PlannedSessionStatus.PLANNED);
        assertThat(replacement.getPlannedSession()).isEqualTo(replacementSession());
    }

    @Test
    void replace_planned_session_rejects_a_session_from_another_plan() {
        AdaptiveRunningPlan plan = plan(persistedSession());

        assertThatThrownBy(() -> plan.replacePlannedSession(99L, replacementSession()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Planned session does not belong to the plan");
    }

    @Test
    void replace_planned_session_rejects_a_missing_replacement_without_changing_the_original() {
        AdaptiveRunningPlan plan = plan(persistedSession());

        assertThatThrownBy(() -> plan.replacePlannedSession(10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Replacement planned session is required");
        assertThat(plan.getPlannedSessions().getFirst().getStatus()).isEqualTo(PlannedSessionStatus.PLANNED);
    }

    private AdaptiveRunningPlan plan(AdaptiveRunningPlanSession session) {
        return new AdaptiveRunningPlan(
                1L,
                "runner@sudolife.com",
                new RunningGoal(10.0, 330, LocalDate.parse("2026-10-01")),
                "A progressive plan based on recent running history.",
                Instant.parse("2026-07-27T12:00:00Z"),
                List.of(session)
        );
    }

    private AdaptiveRunningPlanSession persistedSession() {
        return new AdaptiveRunningPlanSession(10L, null, originalSession(), PlannedSessionStatus.PLANNED);
    }

    private PlannedSessionResult originalSession() {
        return new PlannedSessionResult(
                1,
                1,
                PlannedSessionType.EASY_RUN,
                5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse("2026-08-03")
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
