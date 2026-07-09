package com.sudolife.application.service.strava.activity;

import com.sudolife.application.model.strava.StravaActivityType;

import java.time.Instant;

public record StravaActivitySummaryImport(Long sourceActivityId, StravaActivityType activityType, String rawSportType,
                                          String name, Instant startDate, Double distanceMeters,
                                          Integer movingTimeSeconds, Double averageSpeedMetersPerSecond,
                                          Double totalElevationGainMeters, Double maxSpeedMetersPerSecond,
                                          Double averageHeartRate, Double maxHeartRate, Double averageCadence,
                                          Double averageWatts, Double calories) {
}
