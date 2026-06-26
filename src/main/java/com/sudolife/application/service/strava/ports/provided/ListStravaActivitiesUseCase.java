package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.ListStravaActivitiesCommand;
import com.sudolife.application.service.strava.StravaActivityListResult;

public interface ListStravaActivitiesUseCase {

    StravaActivityListResult execute(ListStravaActivitiesCommand command);
}
