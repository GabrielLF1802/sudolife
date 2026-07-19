package com.sudolife.application.model.training;

import lombok.Getter;

import java.time.Instant;
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
        this.plannedSessions = List.copyOf(plannedSessions);
    }
}
