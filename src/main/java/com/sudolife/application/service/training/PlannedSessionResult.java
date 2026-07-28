package com.sudolife.application.service.training;

import java.time.LocalDate;

public record PlannedSessionResult(
        int weekNumber,
        int sessionNumber,
        PlannedSessionType type,
        double distanceKilometers,
        PlannedSessionTargetResult target,
        LocalDate scheduledDate,
        Integer durationSeconds
) {

    public PlannedSessionResult(
            int weekNumber,
            int sessionNumber,
            PlannedSessionType type,
            double distanceKilometers,
            PlannedSessionTargetResult target,
            LocalDate scheduledDate
    ) {
        this(weekNumber, sessionNumber, type, distanceKilometers, target, scheduledDate, null);
    }
}
