package com.sudolife.application.service.training;

import java.time.Instant;
import java.util.List;

public record RunningHistorySnapshotResult(
        boolean sufficientRunningHistory,
        int activeWeeks,
        int runningActivityCount,
        double totalDistanceKilometers,
        long totalMovingTimeSeconds,
        Instant latestRunAt,
        List<WeeklyRunningVolumeResult> weeklyRunningVolumes,
        double averageRunsPerWeek,
        double longestRunKilometers,
        Double representativePaceSecondsPerKilometer,
        RunningVolumeTrend volumeTrend
) {

    public RunningHistorySnapshotResult(
            boolean sufficientRunningHistory,
            int activeWeeks,
            int runningActivityCount,
            double totalDistanceKilometers,
            long totalMovingTimeSeconds,
            Instant latestRunAt
    ) {
        this(sufficientRunningHistory, activeWeeks, runningActivityCount, totalDistanceKilometers,
                totalMovingTimeSeconds, latestRunAt, List.of(), 0, 0, null,
                RunningVolumeTrend.INSUFFICIENT_DATA);
    }
}
