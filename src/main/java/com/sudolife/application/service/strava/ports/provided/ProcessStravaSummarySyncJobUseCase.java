package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.ProcessStravaSummarySyncJobCommand;

public interface ProcessStravaSummarySyncJobUseCase {

    void execute(ProcessStravaSummarySyncJobCommand command);
}
