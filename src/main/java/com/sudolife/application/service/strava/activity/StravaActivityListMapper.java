package com.sudolife.application.service.strava.activity;

import com.sudolife.application.model.strava.StravaActivitySummary;
import org.springframework.stereotype.Component;

@Component
public class StravaActivityListMapper {

    private final StravaActivityStreamEligibility streamEligibility = new StravaActivityStreamEligibility();

    public StravaActivityListItemResult toResult(StravaActivitySummary activity) {
        return toResult(activity, false);
    }

    public StravaActivityListItemResult toResult(StravaActivitySummary activity, boolean hasStreamSnapshot) {
        return new StravaActivityListItemResult(activity.getId(), activity.getSourceActivityId(), activity.getName(),
                activity.getActivityType(), activity.getStartDate(), activity.getDistanceMeters(),
                activity.getMovingTimeSeconds(), activity.getAverageSpeedMetersPerSecond(),
                activity.getPaceSecondsPerKilometer(), streamStatus(activity, hasStreamSnapshot));
    }

    private StravaActivityStreamStatus streamStatus(StravaActivitySummary activity, boolean hasStreamSnapshot) {
        if (!streamEligibility.requiresStream(activity.getActivityType())) {
            return StravaActivityStreamStatus.NOT_REQUIRED;
        }

        if (hasStreamSnapshot) {
            return StravaActivityStreamStatus.COMPLETED;
        }

        return StravaActivityStreamStatus.PENDING;
    }
}
