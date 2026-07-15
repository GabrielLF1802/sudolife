package com.sudolife.application.service.training;

import com.sudolife.application.model.training.TrainingHeartRateZone;

import java.util.List;

public record TrainingSnapshot(
        RunningHistorySnapshotResult runningHistory,
        RunningGoalResult safeMilestone,
        List<TrainingHeartRateZone> heartRateZones,
        List<PlannedSessionResult> safeSessions
) {

    public TrainingSnapshot {
        heartRateZones = List.copyOf(heartRateZones);
        safeSessions = List.copyOf(safeSessions);
    }
}
