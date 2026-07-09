package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.activity.GetStravaActivityDetailCommand;
import com.sudolife.application.service.strava.activity.StravaActivityDetailResult;

public interface GetStravaActivityDetailUseCase {

    StravaActivityDetailResult execute(GetStravaActivityDetailCommand command);
}
