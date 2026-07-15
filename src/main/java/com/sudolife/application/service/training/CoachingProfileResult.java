package com.sudolife.application.service.training;

import java.time.LocalDate;
import java.util.List;

public record CoachingProfileResult(
        Double targetDistanceKilometers,
        Integer targetPaceSecondsPerKilometer,
        LocalDate targetDate,
        String readiness,
        boolean injuryConcern,
        List<String> preferredRunningDays,
        boolean configured
) {

    public static CoachingProfileResult missing() {
        return new CoachingProfileResult(null, null, null, null, false, List.of(), false);
    }
}
