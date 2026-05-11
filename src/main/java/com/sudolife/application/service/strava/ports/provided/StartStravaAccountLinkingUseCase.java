package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.StartStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.StravaAuthorizationUrlResult;

public interface StartStravaAccountLinkingUseCase {

    StravaAuthorizationUrlResult execute(StartStravaAccountLinkingCommand command);
}
