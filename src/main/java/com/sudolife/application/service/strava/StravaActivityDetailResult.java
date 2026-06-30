package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaActivityType;

import java.time.Instant;
import java.util.List;

public record StravaActivityDetailResult(Long id, Long sourceActivityId, String name,
                                         StravaActivityType sportType, Instant startDate, Double distanceMeters,
                                         Integer movingTimeSeconds, Double totalElevationGainMeters,
                                         Double averageSpeedMetersPerSecond,
                                         Double averagePaceSecondsPerKilometer,
                                         Double maxSpeedMetersPerSecond, Double averageHeartRate,
                                         Double maxHeartRate, Double averageCadence, Double averageWatts,
                                         Double calories, StravaActivityStreamStatus streamStatus,
                                         List<String> availableStreamMetricNames,
                                         StravaActivityDetailEnrichmentStatus enrichmentStatus) {
}
