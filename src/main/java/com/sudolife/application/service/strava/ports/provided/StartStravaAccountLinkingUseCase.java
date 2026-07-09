package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.linking.StartStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.linking.StravaAuthorizationUrlResult;

public interface StartStravaAccountLinkingUseCase {

    StravaAuthorizationUrlResult execute(StartStravaAccountLinkingCommand command);
}
