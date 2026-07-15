package com.sudolife.application.service.training;

import java.time.LocalDate;
import java.util.List;

public record SaveCoachingProfileCommand(
        Double targetDistanceKilometers,
        Integer targetPaceSecondsPerKilometer,
        LocalDate targetDate,
        String readiness,
        boolean injuryConcern,
        List<String> preferredRunningDays
) {

    public SaveCoachingProfileCommand(
            Double targetDistanceKilometers,
            Integer targetPaceSecondsPerKilometer,
            LocalDate targetDate,
            String readiness,
            boolean injuryConcern
    ) {
        this(targetDistanceKilometers, targetPaceSecondsPerKilometer, targetDate, readiness, injuryConcern, null);
    }
}
