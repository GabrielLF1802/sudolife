package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.CompleteStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.StravaCallbackResult;

public interface CompleteStravaAccountLinkingUseCase {

    StravaCallbackResult execute(CompleteStravaAccountLinkingCommand command);
}
