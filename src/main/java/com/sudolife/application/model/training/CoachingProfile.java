package com.sudolife.application.model.training;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CoachingProfile {

    private Long id;
    private String userEmail;
    private RunningGoal runningGoal;
    private UserReportedReadiness readiness;
    private boolean injuryConcern;

    public CoachingProfile(
            Long id,
            String userEmail,
            RunningGoal runningGoal,
            UserReportedReadiness readiness,
            boolean injuryConcern
    ) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User email is invalid, null or empty");
        }

        if (runningGoal == null) {
            throw new IllegalArgumentException("Running goal is required");
        }

        if (readiness == null) {
            throw new IllegalArgumentException("Readiness is required");
        }

        this.id = id;
        this.userEmail = userEmail;
        this.runningGoal = runningGoal;
        this.readiness = readiness;
        this.injuryConcern = injuryConcern;
    }

    public double getTargetDistanceKilometers() {
        return runningGoal.getTargetDistanceKilometers();
    }

    public Integer getTargetPaceSecondsPerKilometer() {
        return runningGoal.getTargetPaceSecondsPerKilometer();
    }

    public LocalDate getTargetDate() {
        return runningGoal.getTargetDate();
    }
}
