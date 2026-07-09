package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.activity.ListStravaActivitiesCommand;
import com.sudolife.application.service.strava.activity.StravaActivityListResult;

public interface ListStravaActivitiesUseCase {

    StravaActivityListResult execute(ListStravaActivitiesCommand command);
}
