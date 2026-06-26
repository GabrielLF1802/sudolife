package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import org.springframework.stereotype.Component;

@Component
public class StravaActivityListMapper {

    public StravaActivityListItemResult toResult(StravaActivitySummary activity) {
        return new StravaActivityListItemResult(activity.getId(), activity.getSourceActivityId(), activity.getName(),
                activity.getActivityType(), activity.getStartDate(), activity.getDistanceMeters(),
                activity.getMovingTimeSeconds(), activity.getAverageSpeedMetersPerSecond(),
                activity.getPaceSecondsPerKilometer(), streamStatus(activity));
    }

    private StravaActivityStreamStatus streamStatus(StravaActivitySummary activity) {
        if (activity.getActivityType() == StravaActivityType.WEIGHT_TRAINING) {
            return StravaActivityStreamStatus.NOT_REQUIRED;
        }

        return StravaActivityStreamStatus.PENDING;
    }
}
