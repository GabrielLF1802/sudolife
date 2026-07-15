package com.sudolife.application.service.training;

import java.util.List;

public record AiRunningPlanProposal(List<PlannedSessionResult> plannedSessions, String explanation) {

    public AiRunningPlanProposal {
        plannedSessions = List.copyOf(plannedSessions);
    }
}
