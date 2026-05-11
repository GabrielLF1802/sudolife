package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.UnlinkStravaAccountCommand;
import com.sudolife.application.service.strava.UnlinkStravaAccountResult;

public interface UnlinkStravaAccountUseCase {

    UnlinkStravaAccountResult execute(UnlinkStravaAccountCommand command);
}
