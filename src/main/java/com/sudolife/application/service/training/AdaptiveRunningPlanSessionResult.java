package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlanSession;

public record AdaptiveRunningPlanSessionResult(
        Long id,
        Long originalPlannedSessionId,
        PlannedSessionResult plannedSession,
        PlannedSessionStatus status
) {

    public static AdaptiveRunningPlanSessionResult from(AdaptiveRunningPlanSession session) {
        return new AdaptiveRunningPlanSessionResult(
                session.getId(),
                session.getOriginalPlannedSessionId(),
                session.getPlannedSession(),
                session.getStatus());
    }
}
