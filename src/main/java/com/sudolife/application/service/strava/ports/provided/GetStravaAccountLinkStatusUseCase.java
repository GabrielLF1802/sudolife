package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.GetStravaAccountLinkStatusCommand;
import com.sudolife.application.service.strava.StravaLinkStatusResult;

public interface GetStravaAccountLinkStatusUseCase {

    StravaLinkStatusResult execute(GetStravaAccountLinkStatusCommand command);
}
