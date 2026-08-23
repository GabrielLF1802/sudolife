package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.consent.GetStravaDataConsentStatusCommand;
import com.sudolife.application.service.strava.consent.StravaDataConsentStatusResult;

public interface GetStravaDataConsentStatusUseCase {

    StravaDataConsentStatusResult execute(GetStravaDataConsentStatusCommand command);
}
