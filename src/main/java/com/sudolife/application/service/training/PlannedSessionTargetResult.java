package com.sudolife.application.service.training;

public record PlannedSessionTargetResult(
        PlannedSessionTargetType type,
        Integer minimumHeartRate,
        Integer maximumHeartRate,
        Integer minimumPerceivedEffort,
        Integer maximumPerceivedEffort
) {

    public static PlannedSessionTargetResult heartRate(int minimumHeartRate, int maximumHeartRate) {
        return new PlannedSessionTargetResult(
                PlannedSessionTargetType.HEART_RATE, minimumHeartRate, maximumHeartRate, null, null);
    }

    public static PlannedSessionTargetResult perceivedEffort(int maximumPerceivedEffort) {
        return perceivedEffort(2, maximumPerceivedEffort);
    }

    public static PlannedSessionTargetResult perceivedEffort(
            int minimumPerceivedEffort,
            int maximumPerceivedEffort
    ) {
        return new PlannedSessionTargetResult(
                PlannedSessionTargetType.PERCEIVED_EFFORT,
                null,
                null,
                minimumPerceivedEffort,
                maximumPerceivedEffort
        );
    }
}
