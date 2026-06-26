package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.EnqueueStravaSummarySyncCommand;
import com.sudolife.application.service.strava.EnqueueStravaSummarySyncResult;

public interface EnqueueStravaSummarySyncUseCase {

    EnqueueStravaSummarySyncResult execute(EnqueueStravaSummarySyncCommand command);
}
