package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlanSession;

public record AdaptiveRunningPlanSessionResult(
        Long id,
        Long originalPlannedSessionId,
        PlannedSessionResult plannedSession,
        PlannedSessionStatus status,
        AdaptationTrigger adaptationTrigger,
        Long matchedActivityId,
        Integer postSessionPerceivedEffort
) {

    public AdaptiveRunningPlanSessionResult(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status,
            AdaptationTrigger adaptationTrigger
    ) {
        this(id, originalPlannedSessionId, plannedSession, status, adaptationTrigger, null, null);
    }

    public AdaptiveRunningPlanSessionResult(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status,
            AdaptationTrigger adaptationTrigger,
            Long matchedActivityId
    ) {
        this(id, originalPlannedSessionId, plannedSession, status, adaptationTrigger, matchedActivityId, null);
    }

    public static AdaptiveRunningPlanSessionResult from(AdaptiveRunningPlanSession session) {
        return new AdaptiveRunningPlanSessionResult(
                session.getId(),
                session.getOriginalPlannedSessionId(),
                session.getPlannedSession(),
                session.getStatus(),
                session.getAdaptationTrigger(),
                session.getMatchedActivityId(),
                session.getPostSessionPerceivedEffort());
    }
}
