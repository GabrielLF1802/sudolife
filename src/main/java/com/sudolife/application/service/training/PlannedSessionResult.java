package com.sudolife.application.service.training;

public record PlannedSessionResult(
        int weekNumber,
        int sessionNumber,
        PlannedSessionType type,
        double distanceKilometers,
        PlannedSessionTargetResult target
) {
}
