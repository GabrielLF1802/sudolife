package com.sudolife.application.model.training;

import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.PlannedSessionStatus;
import com.sudolife.application.service.training.AdaptationTrigger;
import lombok.Getter;

@Getter
public class AdaptiveRunningPlanSession {

    private final Long id;
    private final Long originalPlannedSessionId;
    private final PlannedSessionResult plannedSession;
    private final AdaptationTrigger adaptationTrigger;
    private PlannedSessionStatus status;

    public AdaptiveRunningPlanSession(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status,
            AdaptationTrigger adaptationTrigger
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
        this.adaptationTrigger = adaptationTrigger;
    }

    public AdaptiveRunningPlanSession(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status
    ) {
        this(id, originalPlannedSessionId, plannedSession, status, null);
    }

    public static AdaptiveRunningPlanSession planned(PlannedSessionResult plannedSession) {
        return new AdaptiveRunningPlanSession(null, null, plannedSession, PlannedSessionStatus.PLANNED, null);
    }

    public static AdaptiveRunningPlanSession replacementOf(
            AdaptiveRunningPlanSession original,
            PlannedSessionResult replacement
    ) {
        return replacementOf(original, replacement, null);
    }

    public static AdaptiveRunningPlanSession replacementOf(
            AdaptiveRunningPlanSession original,
            PlannedSessionResult replacement,
            AdaptationTrigger adaptationTrigger
    ) {
        if (original == null || original.getId() == null) {
            throw new IllegalArgumentException("A persisted original planned session is required");
        }

        if (original.getStatus() != PlannedSessionStatus.REPLACED) {
            throw new IllegalArgumentException("Original planned session must be replaced");
        }

        return new AdaptiveRunningPlanSession(
                null,
                original.getId(),
                replacement,
                PlannedSessionStatus.PLANNED,
                adaptationTrigger
        );
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
