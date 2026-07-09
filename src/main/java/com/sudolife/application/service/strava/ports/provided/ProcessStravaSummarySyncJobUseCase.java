package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.sync.ProcessStravaSummarySyncJobCommand;

public interface ProcessStravaSummarySyncJobUseCase {

    void execute(ProcessStravaSummarySyncJobCommand command);
}
