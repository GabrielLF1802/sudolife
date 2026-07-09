package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.linking.CompleteStravaAccountLinkingCommand;
import com.sudolife.application.service.strava.linking.StravaCallbackResult;

public interface CompleteStravaAccountLinkingUseCase {

    StravaCallbackResult execute(CompleteStravaAccountLinkingCommand command);
}
