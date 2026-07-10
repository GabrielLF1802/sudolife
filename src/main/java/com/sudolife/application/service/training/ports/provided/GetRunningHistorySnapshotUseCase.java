package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.RunningHistorySnapshotResult;

public interface GetRunningHistorySnapshotUseCase {

    RunningHistorySnapshotResult execute(String userEmail);
}
