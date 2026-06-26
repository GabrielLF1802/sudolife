package com.sudolife.application.model.strava;

import lombok.Getter;

import java.time.Instant;

@Getter
public class StravaActivitySummary {

    private static final double METERS_PER_KILOMETER = 1000.0;

    private Long id;
    private String userEmail;
    private Long accountLinkId;
    private Long sourceActivityId;
    private StravaActivityType activityType;
    private String rawSportType;
    private String name;
    private Instant startDate;
    private Double distanceMeters;
    private Integer movingTimeSeconds;
    private Double averageSpeedMetersPerSecond;
    private Double paceSecondsPerKilometer;
    private Double totalElevationGainMeters;
    private Double maxSpeedMetersPerSecond;
    private Double averageHeartRate;
    private Double maxHeartRate;
    private Double averageCadence;
    private Double averageWatts;
    private Double calories;
    private Instant importedAt;

    public StravaActivitySummary(Long id, String userEmail, Long accountLinkId, Long sourceActivityId,
                                 StravaActivityType activityType, String rawSportType, String name, Instant startDate,
                                 Double distanceMeters, Integer movingTimeSeconds, Double averageSpeedMetersPerSecond,
                                 Double paceSecondsPerKilometer, Double totalElevationGainMeters,
                                 Double maxSpeedMetersPerSecond, Double averageHeartRate, Double maxHeartRate,
                                 Double averageCadence, Double averageWatts, Double calories, Instant importedAt) {
        validateText(userEmail, "User email is invalid");
        validateNumber(accountLinkId, "Account link id is invalid");
        validateNumber(sourceActivityId, "Source activity id is invalid");
        validateActivityType(activityType);
        validateText(rawSportType, "Raw sport type is invalid");
        validateText(name, "Activity name is invalid");
        validateInstant(startDate, "Start date cant be null");
        validateInstant(importedAt, "Imported at cant be null");

        this.id = id;
        this.userEmail = userEmail;
        this.accountLinkId = accountLinkId;
        this.sourceActivityId = sourceActivityId;
        this.activityType = activityType;
        this.rawSportType = rawSportType;
        this.name = name;
        this.startDate = startDate;
        this.distanceMeters = distanceMeters;
        this.movingTimeSeconds = movingTimeSeconds;
        this.averageSpeedMetersPerSecond = averageSpeedMetersPerSecond;
        this.paceSecondsPerKilometer = paceSecondsPerKilometer;
        this.totalElevationGainMeters = totalElevationGainMeters;
        this.maxSpeedMetersPerSecond = maxSpeedMetersPerSecond;
        this.averageHeartRate = averageHeartRate;
        this.maxHeartRate = maxHeartRate;
        this.averageCadence = averageCadence;
        this.averageWatts = averageWatts;
        this.calories = calories;
        this.importedAt = importedAt;
    }

    public static StravaActivitySummary imported(String userEmail, Long accountLinkId, Long sourceActivityId,
                                                 StravaActivityType activityType, String rawSportType, String name,
                                                 Instant startDate, Double distanceMeters, Integer movingTimeSeconds,
                                                 Double averageSpeedMetersPerSecond, Double totalElevationGainMeters,
                                                 Double maxSpeedMetersPerSecond, Double averageHeartRate,
                                                 Double maxHeartRate, Double averageCadence, Double averageWatts,
                                                 Double calories, Instant importedAt) {
        return new StravaActivitySummary(null, userEmail, accountLinkId, sourceActivityId, activityType, rawSportType,
                name, startDate, distanceMeters, movingTimeSeconds, averageSpeedMetersPerSecond,
                paceSecondsPerKilometer(distanceMeters, movingTimeSeconds), totalElevationGainMeters,
                maxSpeedMetersPerSecond, averageHeartRate, maxHeartRate, averageCadence, averageWatts, calories,
                importedAt);
    }

    private static Double paceSecondsPerKilometer(Double distanceMeters, Integer movingTimeSeconds) {
        if (distanceMeters == null || distanceMeters <= 0 || movingTimeSeconds == null) {
            return null;
        }

        return movingTimeSeconds / (distanceMeters / METERS_PER_KILOMETER);
    }

    private void validateText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateNumber(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateActivityType(StravaActivityType value) {
        if (value == null) {
            throw new IllegalArgumentException("Activity type cant be null");
        }
    }

    private void validateInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
