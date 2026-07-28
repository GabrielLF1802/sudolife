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
    private Long matchedActivityId;
    private Integer postSessionPerceivedEffort;

    public AdaptiveRunningPlanSession(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status,
            AdaptationTrigger adaptationTrigger,
            Long matchedActivityId,
            Integer postSessionPerceivedEffort
    ) {
        if (plannedSession == null) {
            throw new IllegalArgumentException("Planned session is required");
        }

        if (status == null) {
            throw new IllegalArgumentException("Planned session status is required");
        }

        validatePostSessionPerceivedEffort(status, postSessionPerceivedEffort);

        this.id = id;
        this.originalPlannedSessionId = originalPlannedSessionId;
        this.plannedSession = plannedSession;
        this.status = status;
        this.adaptationTrigger = adaptationTrigger;
        this.matchedActivityId = matchedActivityId;
        this.postSessionPerceivedEffort = postSessionPerceivedEffort;
    }

    public AdaptiveRunningPlanSession(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status
    ) {
        this(id, originalPlannedSessionId, plannedSession, status, null, null, null);
    }

    public AdaptiveRunningPlanSession(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status,
            AdaptationTrigger adaptationTrigger
    ) {
        this(id, originalPlannedSessionId, plannedSession, status, adaptationTrigger, null, null);
    }

    public AdaptiveRunningPlanSession(
            Long id,
            Long originalPlannedSessionId,
            PlannedSessionResult plannedSession,
            PlannedSessionStatus status,
            AdaptationTrigger adaptationTrigger,
            Long matchedActivityId
    ) {
        this(id, originalPlannedSessionId, plannedSession, status, adaptationTrigger, matchedActivityId, null);
    }

    public static AdaptiveRunningPlanSession planned(PlannedSessionResult plannedSession) {
        return new AdaptiveRunningPlanSession(null, null, plannedSession, PlannedSessionStatus.PLANNED, null, null,
                null);
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
                adaptationTrigger,
                null,
                null
        );
    }

    public void markReplaced() {
        changeStatus(PlannedSessionStatus.REPLACED);
    }

    public void markCompleted() {
        changeStatus(PlannedSessionStatus.COMPLETED);
    }

    public void match(Long activityId) {
        if (activityId == null) {
            throw new IllegalArgumentException("Matched activity id is required");
        }

        if (status != PlannedSessionStatus.PLANNED) {
            throw new IllegalStateException("Only planned sessions can be matched");
        }

        matchedActivityId = activityId;
        status = PlannedSessionStatus.COMPLETED;
    }

    public void unlinkMatch() {
        if (status != PlannedSessionStatus.COMPLETED || matchedActivityId == null) {
            throw new IllegalStateException("Only matched sessions can be unlinked");
        }

        matchedActivityId = null;
        status = PlannedSessionStatus.PLANNED;
    }

    public void markMissed() {
        changeStatus(PlannedSessionStatus.MISSED);
    }

    public void reportPostSessionPerceivedEffort(int perceivedEffort) {
        if (status != PlannedSessionStatus.COMPLETED) {
            throw new IllegalStateException("Perceived effort requires a completed planned session");
        }

        if (perceivedEffort < 1 || perceivedEffort > 10) {
            throw new IllegalArgumentException("Perceived effort must be between 1 and 10");
        }

        postSessionPerceivedEffort = perceivedEffort;
    }

    private void changeStatus(PlannedSessionStatus newStatus) {
        if (status != PlannedSessionStatus.PLANNED) {
            throw new IllegalStateException("Only planned sessions can change status");
        }

        status = newStatus;
    }

    private void validatePostSessionPerceivedEffort(
            PlannedSessionStatus plannedSessionStatus,
            Integer perceivedEffort
    ) {
        if (perceivedEffort == null) {
            return;
        }

        if (plannedSessionStatus != PlannedSessionStatus.COMPLETED) {
            throw new IllegalArgumentException("Perceived effort requires a completed planned session");
        }

        if (perceivedEffort < 1 || perceivedEffort > 10) {
            throw new IllegalArgumentException("Perceived effort must be between 1 and 10");
        }
    }
}
