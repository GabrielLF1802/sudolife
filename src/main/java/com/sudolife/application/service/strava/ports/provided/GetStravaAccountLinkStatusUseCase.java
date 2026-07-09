package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.linking.GetStravaAccountLinkStatusCommand;
import com.sudolife.application.service.strava.linking.StravaLinkStatusResult;

public interface GetStravaAccountLinkStatusUseCase {

    StravaLinkStatusResult execute(GetStravaAccountLinkStatusCommand command);
}
