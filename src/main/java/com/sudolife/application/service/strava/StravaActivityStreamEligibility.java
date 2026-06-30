package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaActivityType;

public class StravaActivityStreamEligibility {

    public boolean requiresStream(StravaActivityType activityType) {
        return activityType == StravaActivityType.RUN || activityType == StravaActivityType.WALK ||
                activityType == StravaActivityType.RIDE;
    }
}
