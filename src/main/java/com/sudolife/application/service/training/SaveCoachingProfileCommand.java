package com.sudolife.application.service.training;

import java.time.LocalDate;

public record SaveCoachingProfileCommand(
        Double targetDistanceKilometers,
        Integer targetPaceSecondsPerKilometer,
        LocalDate targetDate,
        String readiness,
        boolean injuryConcern
) {
}
