package com.sudolife.application.service.training;

import java.time.Instant;

public record RunningHistorySnapshotResult(
        boolean sufficientRunningHistory,
        int activeWeeks,
        int runningActivityCount,
        double totalDistanceKilometers,
        long totalMovingTimeSeconds,
        Instant latestRunAt
) {
}
