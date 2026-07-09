package com.sudolife.application.service.training;

import java.time.LocalDate;

public record CoachingProfileResult(
        Double targetDistanceKilometers,
        Integer targetPaceSecondsPerKilometer,
        LocalDate targetDate,
        String readiness,
        boolean injuryConcern,
        boolean configured
) {

    public static CoachingProfileResult missing() {
        return new CoachingProfileResult(null, null, null, null, false, false);
    }
}
