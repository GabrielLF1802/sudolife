package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PlannedSessionMatchPolicyUnitTest {

    private final PlannedSessionMatchPolicy policy = new PlannedSessionMatchPolicy();

    @Test
    void matches_run_within_date_distance_and_duration_tolerances() {
        PlannedSessionResult session = plannedSession();
        StravaActivitySummary activity = activity(StravaActivityType.RUN, "2026-08-03T09:00:00Z", 5250.0, 1890);

        boolean result = policy.matches(session, activity);

        assertThat(result).isTrue();
    }

    @Test
    void rejects_non_running_activity() {
        PlannedSessionResult session = plannedSession();
        StravaActivitySummary activity = activity(StravaActivityType.RIDE, "2026-08-03T09:00:00Z", 5000.0, 1800);

        boolean result = policy.matches(session, activity);

        assertThat(result).isFalse();
    }

    @Test
    void rejects_activity_outside_duration_tolerance() {
        PlannedSessionResult session = plannedSession();
        StravaActivitySummary activity = activity(StravaActivityType.RUN, "2026-08-03T09:00:00Z", 5000.0, 2400);

        boolean result = policy.matches(session, activity);

        assertThat(result).isFalse();
    }

    @Test
    void closest_candidate_prefers_smallest_combined_distance_and_duration_difference() {
        PlannedSessionResult session = plannedSession();
        StravaActivitySummary farther = activity(StravaActivityType.RUN, "2026-08-03T08:00:00Z", 5500.0, 1980);
        StravaActivitySummary closer = activity(StravaActivityType.RUN, "2026-08-03T10:00:00Z", 5050.0, 1810);

        StravaActivitySummary result = policy.closestCandidate(session, java.util.List.of(farther, closer))
                .orElseThrow();

        assertThat(result).isSameAs(closer);
    }

    private PlannedSessionResult plannedSession() {
        return new PlannedSessionResult(1, 1, PlannedSessionType.EASY_RUN, 5.0,
                PlannedSessionTargetResult.perceivedEffort(2, 4), LocalDate.parse("2026-08-03"), 1800);
    }

    private StravaActivitySummary activity(
            StravaActivityType type,
            String startDate,
            double distanceMeters,
            int movingTimeSeconds
    ) {
        return new StravaActivitySummary(10L, "runner@sudolife.com", 20L, 30L, type, type.name(), "Activity",
                Instant.parse(startDate), distanceMeters, movingTimeSeconds, 3.0, 360.0, 10.0, 4.0,
                150.0, 170.0, 80.0, 200.0, 300.0, Instant.parse("2026-08-03T11:00:00Z"));
    }
}
