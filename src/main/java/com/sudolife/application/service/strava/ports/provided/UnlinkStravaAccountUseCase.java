package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.linking.UnlinkStravaAccountCommand;
import com.sudolife.application.service.strava.linking.UnlinkStravaAccountResult;

public interface UnlinkStravaAccountUseCase {

    UnlinkStravaAccountResult execute(UnlinkStravaAccountCommand command);
}
