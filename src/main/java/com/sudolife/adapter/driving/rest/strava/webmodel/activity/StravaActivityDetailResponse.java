package com.sudolife.adapter.driving.rest.strava.webmodel.activity;

import java.time.Instant;
import java.util.List;

public record StravaActivityDetailResponse(Long id, Long sourceActivityId, String name, String sportType,
                                           Instant startDate, Double distanceMeters, Integer movingTimeSeconds,
                                           Double totalElevationGainMeters,
                                           Double averageSpeedMetersPerSecond,
                                           Double averagePaceSecondsPerKilometer,
                                           Double maxSpeedMetersPerSecond, Double averageHeartRate,
                                           Double maxHeartRate, Double averageCadence, Double averageWatts,
                                           Double calories, String streamStatus,
                                           List<String> availableStreamMetricNames, String enrichmentStatus) {
}
