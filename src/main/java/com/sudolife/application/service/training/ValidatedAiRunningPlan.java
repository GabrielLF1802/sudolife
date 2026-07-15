package com.sudolife.application.service.training;

import java.util.List;

public record ValidatedAiRunningPlan(List<PlannedSessionResult> plannedSessions, boolean adjusted) {

    public ValidatedAiRunningPlan {
        plannedSessions = List.copyOf(plannedSessions);
    }
}
