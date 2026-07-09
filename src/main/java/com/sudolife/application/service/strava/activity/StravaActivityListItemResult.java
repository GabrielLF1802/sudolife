package com.sudolife.application.service.strava.activity;

import com.sudolife.application.model.strava.StravaActivityType;

import java.time.Instant;

public record StravaActivityListItemResult(Long id, Long sourceActivityId, String name,
                                           StravaActivityType sportType, Instant startDate, Double distanceMeters,
                                           Integer movingTimeSeconds, Double averageSpeedMetersPerSecond,
                                           Double averagePaceSecondsPerKilometer,
                                           StravaActivityStreamStatus streamStatus) {
}
