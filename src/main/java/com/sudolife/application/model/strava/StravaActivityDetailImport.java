package com.sudolife.application.model.strava;

import java.time.Instant;

public record StravaActivityDetailImport(Long sourceActivityId, StravaActivityType activityType,
                                         String rawSportType, String name, Instant startDate,
                                         Double distanceMeters, Integer movingTimeSeconds,
                                         Double averageSpeedMetersPerSecond, Double totalElevationGainMeters,
                                         Double maxSpeedMetersPerSecond, Double averageHeartRate,
                                         Double maxHeartRate, Double averageCadence, Double averageWatts,
                                         Double calories) {
}
