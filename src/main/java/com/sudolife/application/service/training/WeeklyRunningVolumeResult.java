package com.sudolife.application.service.training;

public record WeeklyRunningVolumeResult(
        int weeksAgo,
        int runningActivityCount,
        double distanceKilometers,
        long movingTimeSeconds
) {
}
