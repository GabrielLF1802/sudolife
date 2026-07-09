package com.sudolife.application.service.training;

import com.sudolife.application.model.training.TrainingHeartRateZone;

import java.util.List;

public record TrainingProfileResult(Integer birthYear, boolean adaptiveCoachingEligible,
                                    TrainingHeartRateZoneSource heartRateZoneSource,
                                    List<TrainingHeartRateZone> heartRateZones) {

    public static TrainingProfileResult missing() {
        return new TrainingProfileResult(null, false, TrainingHeartRateZoneSource.UNAVAILABLE, List.of());
    }

    public static TrainingProfileResult existing(Integer birthYear, List<TrainingHeartRateZone> importedHeartRateZones, int currentYear) {
        if (importedHeartRateZones == null || importedHeartRateZones.isEmpty()) {
            if (birthYear == null) {
                return missing();
            }

            return new TrainingProfileResult(birthYear, true, TrainingHeartRateZoneSource.AGE_BASED,
                    ageBasedHeartRateZones(birthYear, currentYear));
        }

        return new TrainingProfileResult(birthYear, birthYear != null, TrainingHeartRateZoneSource.STRAVA,
                List.copyOf(importedHeartRateZones));
    }

    private static List<TrainingHeartRateZone> ageBasedHeartRateZones(int birthYear, int currentYear) {
        int maximumHeartRate = 220 - (currentYear - birthYear);

        return List.of(
                percentZone(maximumHeartRate, 50, 60),
                percentZone(maximumHeartRate, 60, 70),
                percentZone(maximumHeartRate, 70, 80),
                percentZone(maximumHeartRate, 80, 90),
                percentZone(maximumHeartRate, 90, 100)
        );
    }

    private static TrainingHeartRateZone percentZone(int maximumHeartRate, int minimumPercent, int maximumPercent) {
        return new TrainingHeartRateZone(maximumHeartRate * minimumPercent / 100,
                maximumHeartRate * maximumPercent / 100);
    }
}
