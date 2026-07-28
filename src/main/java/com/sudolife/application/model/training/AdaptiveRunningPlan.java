package com.sudolife.application.model.training;

import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.AdaptationTrigger;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class AdaptiveRunningPlan {

    private final Long id;
    private final String userEmail;
    private final RunningGoal safeMilestone;
    private final String explanation;
    private final Instant acceptedAt;
    private final List<AdaptiveRunningPlanSession> plannedSessions;

    public AdaptiveRunningPlan(
            Long id,
            String userEmail,
            RunningGoal safeMilestone,
            String explanation,
            Instant acceptedAt,
            List<AdaptiveRunningPlanSession> plannedSessions
    ) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User email is required");
        }

        if (safeMilestone == null) {
            throw new IllegalArgumentException("Safe milestone is required");
        }

        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("Plan explanation is required");
        }

        if (acceptedAt == null) {
            throw new IllegalArgumentException("Plan acceptance time is required");
        }

        this.id = id;
        this.userEmail = userEmail;
        this.safeMilestone = safeMilestone;
        this.explanation = explanation;
        this.acceptedAt = acceptedAt;
        if (plannedSessions == null || plannedSessions.isEmpty()) {
            throw new IllegalArgumentException("Planned sessions are required");
        }

        this.plannedSessions = new ArrayList<>(plannedSessions);
    }

    public List<AdaptiveRunningPlanSession> getPlannedSessions() {
        return Collections.unmodifiableList(plannedSessions);
    }

    public AdaptiveRunningPlanSession replacePlannedSession(
            Long originalPlannedSessionId,
            PlannedSessionResult replacement
    ) {
        return replacePlannedSession(originalPlannedSessionId, replacement, null);
    }

    public AdaptiveRunningPlanSession replacePlannedSession(
            Long originalPlannedSessionId,
            PlannedSessionResult replacement,
            AdaptationTrigger adaptationTrigger
    ) {
        if (replacement == null) {
            throw new IllegalArgumentException("Replacement planned session is required");
        }

        AdaptiveRunningPlanSession original = findSession(originalPlannedSessionId);
        original.markReplaced();
        AdaptiveRunningPlanSession adaptedSession = AdaptiveRunningPlanSession.replacementOf(
                original, replacement, adaptationTrigger);
        plannedSessions.add(adaptedSession);

        return adaptedSession;
    }

    public AdaptiveRunningPlanSession findPlannedSession(Long plannedSessionId) {
        return findSession(plannedSessionId);
    }

    private AdaptiveRunningPlanSession findSession(Long plannedSessionId) {
        if (plannedSessionId == null) {
            throw new IllegalArgumentException("Planned session id is required");
        }

        return plannedSessions.stream()
                .filter(session -> plannedSessionId.equals(session.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Planned session does not belong to the plan"));
    }
}
