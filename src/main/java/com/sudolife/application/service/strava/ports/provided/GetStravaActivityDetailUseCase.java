package com.sudolife.application.service.strava.ports.provided;

import com.sudolife.application.service.strava.GetStravaActivityDetailCommand;
import com.sudolife.application.service.strava.StravaActivityDetailResult;

public interface GetStravaActivityDetailUseCase {

    StravaActivityDetailResult execute(GetStravaActivityDetailCommand command);
}
