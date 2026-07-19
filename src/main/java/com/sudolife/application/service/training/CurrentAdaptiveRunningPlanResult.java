package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;

import java.time.Instant;
import java.util.List;

public record CurrentAdaptiveRunningPlanResult(
        Long id,
        RunningGoalResult safeMilestone,
        String explanation,
        Instant acceptedAt,
        List<AdaptiveRunningPlanSessionResult> plannedSessions
) {

    public CurrentAdaptiveRunningPlanResult {
        plannedSessions = List.copyOf(plannedSessions);
    }

    public static CurrentAdaptiveRunningPlanResult from(AdaptiveRunningPlan plan) {
        return new CurrentAdaptiveRunningPlanResult(
                plan.getId(),
                RunningGoalResult.from(plan.getSafeMilestone()),
                plan.getExplanation(),
                plan.getAcceptedAt(),
                plan.getPlannedSessions().stream().map(AdaptiveRunningPlanSessionResult::from).toList());
    }
}
