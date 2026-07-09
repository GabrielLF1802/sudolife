package com.sudolife.application.model.training;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RunningGoal {

    private double targetDistanceKilometers;
    private Integer targetPaceSecondsPerKilometer;
    private LocalDate targetDate;

    public RunningGoal(
            Double targetDistanceKilometers,
            Integer targetPaceSecondsPerKilometer,
            LocalDate targetDate
    ) {
        if (targetDistanceKilometers == null) {
            throw new IllegalArgumentException("Target distance is required");
        }

        if (!Double.isFinite(targetDistanceKilometers) || targetDistanceKilometers <= 0) {
            throw new IllegalArgumentException("Target distance must be greater than zero");
        }

        if (targetPaceSecondsPerKilometer != null && targetPaceSecondsPerKilometer <= 0) {
            throw new IllegalArgumentException("Target pace must be greater than zero");
        }

        this.targetDistanceKilometers = targetDistanceKilometers;
        this.targetPaceSecondsPerKilometer = targetPaceSecondsPerKilometer;
        this.targetDate = targetDate;
    }

    public static RunningGoal createFromUserInput(
            Double targetDistanceKilometers,
            Integer targetPaceSecondsPerKilometer,
            LocalDate targetDate,
            LocalDate currentDate
    ) {
        if (targetDate != null && targetDate.isBefore(currentDate)) {
            throw new IllegalArgumentException("Target date cannot be in the past");
        }

        return new RunningGoal(targetDistanceKilometers, targetPaceSecondsPerKilometer, targetDate);
    }
}
