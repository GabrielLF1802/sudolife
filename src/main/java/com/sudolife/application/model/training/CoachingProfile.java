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
    private RunningAvailability runningAvailability;

    public CoachingProfile(
            Long id,
            String userEmail,
            RunningGoal runningGoal,
            UserReportedReadiness readiness,
            boolean injuryConcern,
            RunningAvailability runningAvailability
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
        this.runningAvailability = runningAvailability == null
                ? new RunningAvailability(null)
                : runningAvailability;
    }

    public CoachingProfile(
            Long id,
            String userEmail,
            RunningGoal runningGoal,
            UserReportedReadiness readiness,
            boolean injuryConcern
    ) {
        this(id, userEmail, runningGoal, readiness, injuryConcern, new RunningAvailability(null));
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
