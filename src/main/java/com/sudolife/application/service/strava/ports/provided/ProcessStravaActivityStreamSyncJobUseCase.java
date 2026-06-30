package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.ProcessStravaActivityStreamSyncJobCommand;

public interface ProcessStravaActivityStreamSyncJobUseCase {

    void execute(ProcessStravaActivityStreamSyncJobCommand command);
}
