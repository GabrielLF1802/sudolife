package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.activity.RequestStravaActivitySyncCommand;
import com.sudolife.application.service.strava.activity.StravaActivitySyncResult;

public interface RequestStravaActivitySyncUseCase {

    StravaActivitySyncResult execute(RequestStravaActivitySyncCommand command);
}
