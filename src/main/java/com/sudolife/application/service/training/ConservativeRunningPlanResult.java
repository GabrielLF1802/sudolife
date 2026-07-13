package com.sudolife.application.service.training;

import java.util.List;

public record ConservativeRunningPlanResult(
        ConservativeRunningPlanClassification classification,
        List<ConservativeRunningPlanReason> reasons,
        double longTermGoalDistanceKilometers,
        int durationWeeks,
        int sessionsPerWeek,
        int weeklyProgressionPercent,
        List<PlannedSessionResult> plannedSessions
) {
}
