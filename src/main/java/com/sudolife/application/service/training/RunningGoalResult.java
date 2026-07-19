package com.sudolife.application.service.training;

import com.sudolife.application.model.training.RunningGoal;

import java.time.LocalDate;

public record RunningGoalResult(
        double targetDistanceKilometers,
        Integer targetPaceSecondsPerKilometer,
        LocalDate targetDate
) {

    public static RunningGoalResult from(RunningGoal runningGoal) {
        return new RunningGoalResult(
                runningGoal.getTargetDistanceKilometers(),
                runningGoal.getTargetPaceSecondsPerKilometer(),
                runningGoal.getTargetDate());
    }
}
