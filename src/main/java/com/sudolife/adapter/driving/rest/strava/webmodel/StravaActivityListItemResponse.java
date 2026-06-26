package com.sudolife.adapter.driving.rest.strava.webmodel;

import java.time.Instant;

public record StravaActivityListItemResponse(Long id, Long sourceActivityId, String name, String sportType,
                                             Instant startDate, Double distanceMeters, Integer movingTimeSeconds,
                                             Double averageSpeedMetersPerSecond,
                                             Double averagePaceSecondsPerKilometer, String streamStatus) {
}
