package com.sudolife.application.model.training;

public record TrainingHeartRateZone(int minimumHeartRate, int maximumHeartRate) {

    public TrainingHeartRateZone {
        if (minimumHeartRate < 0) {
            throw new IllegalArgumentException("Minimum heart rate is invalid");
        }

        if (maximumHeartRate <= minimumHeartRate) {
            throw new IllegalArgumentException("Maximum heart rate must be greater than minimum heart rate");
        }
    }
}
