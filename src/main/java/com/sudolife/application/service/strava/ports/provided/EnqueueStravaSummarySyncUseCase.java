package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.sync.EnqueueStravaSummarySyncCommand;
import com.sudolife.application.service.strava.sync.EnqueueStravaSummarySyncResult;

public interface EnqueueStravaSummarySyncUseCase {

    EnqueueStravaSummarySyncResult execute(EnqueueStravaSummarySyncCommand command);
}
