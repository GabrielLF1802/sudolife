package com.sudolife.application.model.training;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CoachingProfile {

    private Long id;
    private String userEmail;
    private double targetDistanceKilometers;
    private Integer targetPaceSecondsPerKilometer;
    private LocalDate targetDate;
    private UserReportedReadiness readiness;
    private boolean injuryConcern;

    public CoachingProfile(
            Long id,
            String userEmail,
            double targetDistanceKilometers,
            Integer targetPaceSecondsPerKilometer,
            LocalDate targetDate,
            UserReportedReadiness readiness,
            boolean injuryConcern
    ) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User email is invalid, null or empty");
        }

        this.id = id;
        this.userEmail = userEmail;
        this.targetDistanceKilometers = targetDistanceKilometers;
        this.targetPaceSecondsPerKilometer = targetPaceSecondsPerKilometer;
        this.targetDate = targetDate;
        this.readiness = readiness;
        this.injuryConcern = injuryConcern;
    }
}
