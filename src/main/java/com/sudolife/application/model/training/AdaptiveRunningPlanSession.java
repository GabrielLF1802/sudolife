package com.sudolife.application.model.training;

import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.PlannedSessionStatus;
import lombok.Getter;

@Getter
public class AdaptiveRunningPlanSession {

    private final Long id;
    private final Long originalPlannedSessionId;
    private final PlannedSessionResult plannedSession;
    private PlannedSessionStatus status;

    public AdaptiveRunningPlanSession(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status
    ) {
        if (plannedSession == null) {
            throw new IllegalArgumentException("Planned session is required");
        }

        if (status == null) {
            throw new IllegalArgumentException("Planned session status is required");
        }

        this.id = id;
        this.originalPlannedSessionId = originalPlannedSessionId;
        this.plannedSession = plannedSession;
        this.status = status;
    }

    public static AdaptiveRunningPlanSession planned(PlannedSessionResult plannedSession) {
        return new AdaptiveRunningPlanSession(null, null, plannedSession, PlannedSessionStatus.PLANNED);
    }

    public void markReplaced() {
        changeStatus(PlannedSessionStatus.REPLACED);
    }

    public void markCompleted() {
        changeStatus(PlannedSessionStatus.COMPLETED);
    }

    public void markMissed() {
        changeStatus(PlannedSessionStatus.MISSED);
    }

    private void changeStatus(PlannedSessionStatus newStatus) {
        if (status != PlannedSessionStatus.PLANNED) {
            throw new IllegalStateException("Only planned sessions can change status");
        }

        status = newStatus;
    }
}
