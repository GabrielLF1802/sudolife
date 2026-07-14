package com.sudolife.application.service.training;

import java.time.LocalDate;

public record RunningGoalResult(
        double targetDistanceKilometers,
        Integer targetPaceSecondsPerKilometer,
        LocalDate targetDate
) {
}
