package com.sudolife.application.model.training;

import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.PlannedSessionStatus;
import com.sudolife.application.service.training.PlannedSessionTargetResult;
import com.sudolife.application.service.training.PlannedSessionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdaptiveRunningPlanSessionUnitTest {

    @Test
    void mark_completed_changes_a_planned_session_to_completed() {
        AdaptiveRunningPlanSession session = persistedSession(PlannedSessionStatus.PLANNED);

        session.markCompleted();

        assertThat(session.getStatus()).isEqualTo(PlannedSessionStatus.COMPLETED);
    }

    @Test
    void mark_missed_changes_a_planned_session_to_missed() {
        AdaptiveRunningPlanSession session = persistedSession(PlannedSessionStatus.PLANNED);

        session.markMissed();

        assertThat(session.getStatus()).isEqualTo(PlannedSessionStatus.MISSED);
    }

    @Test
    void mark_replaced_changes_a_planned_session_to_replaced() {
        AdaptiveRunningPlanSession session = persistedSession(PlannedSessionStatus.PLANNED);

        session.markReplaced();

        assertThat(session.getStatus()).isEqualTo(PlannedSessionStatus.REPLACED);
    }

    @Test
    void status_transition_from_a_terminal_status_is_rejected() {
        AdaptiveRunningPlanSession session = persistedSession(PlannedSessionStatus.COMPLETED);

        assertThatThrownBy(session::markMissed)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only planned sessions can change status");
    }

    @Test
    void match_completes_session_and_unlink_restores_planned_state() {
        AdaptiveRunningPlanSession session = persistedSession(PlannedSessionStatus.PLANNED);
        session.match(99L);

        session.unlinkMatch();

        assertThat(session.getStatus()).isEqualTo(PlannedSessionStatus.PLANNED);
        assertThat(session.getMatchedActivityId()).isNull();
    }

    private AdaptiveRunningPlanSession persistedSession(PlannedSessionStatus status) {
        return new AdaptiveRunningPlanSession(10L, null, plannedSession(), status);
    }

    private PlannedSessionResult plannedSession() {
        return new PlannedSessionResult(
                1,
                1,
                PlannedSessionType.EASY_RUN,
                5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4),
                LocalDate.parse("2026-08-03")
        );
    }
}
