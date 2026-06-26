package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.RequestStravaActivitySyncCommand;
import com.sudolife.application.service.strava.StravaActivitySyncResult;

public interface RequestStravaActivitySyncUseCase {

    StravaActivitySyncResult execute(RequestStravaActivitySyncCommand command);
}
