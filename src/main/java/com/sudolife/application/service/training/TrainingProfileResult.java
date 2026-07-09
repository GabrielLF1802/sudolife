package com.sudolife.application.service.training;

public record TrainingProfileResult(Integer birthYear, boolean adaptiveCoachingEligible) {

    public static TrainingProfileResult missing() {
        return new TrainingProfileResult(null, false);
    }

    public static TrainingProfileResult existing(int birthYear) {
        return new TrainingProfileResult(birthYear, true);
    }
}
