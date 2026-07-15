package com.sudolife.application.service.training;

import com.sudolife.application.service.training.exception.UnsafeAiRunningPlanException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRunningPlanValidatorUnitTest {

    private final AiRunningPlanValidator validator = new AiRunningPlanValidator();

    @Test
    void validate_with_changed_target_returns_backend_safe_target_and_marks_plan_adjusted() {
        PlannedSessionResult safeSession = session(5.0, PlannedSessionTargetResult.perceivedEffort(2, 4));
        TrainingSnapshot snapshot = snapshot(safeSession);
        AiRunningPlanProposal proposal = new AiRunningPlanProposal(
                List.of(session(5.0, PlannedSessionTargetResult.perceivedEffort(4, 6))), "Explanation");

        ValidatedAiRunningPlan result = validator.validate(snapshot, proposal);

        assertThat(result.adjusted()).isTrue();
        assertThat(result.plannedSessions()).containsExactly(safeSession);
    }

    @Test
    void validate_with_unsafe_distance_rejects_proposal() {
        PlannedSessionResult safeSession = session(5.0, PlannedSessionTargetResult.perceivedEffort(2, 4));
        TrainingSnapshot snapshot = snapshot(safeSession);
        AiRunningPlanProposal proposal = new AiRunningPlanProposal(
                List.of(session(6.0, PlannedSessionTargetResult.perceivedEffort(2, 4))), "Explanation");

        assertThatThrownBy(() -> validator.validate(snapshot, proposal))
                .isInstanceOf(UnsafeAiRunningPlanException.class);
    }

    private TrainingSnapshot snapshot(PlannedSessionResult safeSession) {
        return new TrainingSnapshot(
                new RunningHistorySnapshotResult(true, 3, 4, 20.0, 7200, null),
                new RunningGoalResult(10.0, null, null),
                List.of(),
                List.of(safeSession));
    }

    private PlannedSessionResult session(double distance, PlannedSessionTargetResult target) {
        return new PlannedSessionResult(1, 1, PlannedSessionType.EASY_RUN, distance, target,
                LocalDate.parse("2026-07-21"));
    }
}
