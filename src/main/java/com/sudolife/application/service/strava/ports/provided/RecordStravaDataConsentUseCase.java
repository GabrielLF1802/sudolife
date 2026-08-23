package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.consent.RecordStravaDataConsentCommand;

public interface RecordStravaDataConsentUseCase {

    void execute(RecordStravaDataConsentCommand command);
}
