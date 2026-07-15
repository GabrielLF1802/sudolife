package com.sudolife.application.service.training;

import java.util.List;

public record AdaptiveRunningPlanResult(
        RunningGoalResult safeMilestone,
        List<PlannedSessionResult> plannedSessions,
        String explanation,
        boolean adjustedBySafetyValidation
) {

    public AdaptiveRunningPlanResult {
        plannedSessions = List.copyOf(plannedSessions);
    }
}
