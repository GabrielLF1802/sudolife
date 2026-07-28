package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;

import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public class PlannedSessionMatchPolicy {

    private static final double DISTANCE_TOLERANCE = 0.20;
    private static final double DURATION_TOLERANCE = 0.20;
    private static final long DATE_TOLERANCE_DAYS = 1;

    public boolean matches(PlannedSessionResult session, StravaActivitySummary activity) {
        if (activity.getActivityType() != StravaActivityType.RUN
                || session.durationSeconds() == null
                || session.durationSeconds() <= 0
                || session.distanceKilometers() <= 0
                || activity.getDistanceMeters() == null
                || activity.getMovingTimeSeconds() == null) {
            return false;
        }

        return dateDifference(session, activity) <= DATE_TOLERANCE_DAYS
                && relativeDifference(session.distanceKilometers() * 1000, activity.getDistanceMeters())
                <= DISTANCE_TOLERANCE
                && relativeDifference(session.durationSeconds(), activity.getMovingTimeSeconds())
                <= DURATION_TOLERANCE;
    }

    public Optional<StravaActivitySummary> closestCandidate(
            PlannedSessionResult session,
            Collection<StravaActivitySummary> activities
    ) {
        return activities.stream()
                .filter(activity -> matches(session, activity))
                .min(Comparator.<StravaActivitySummary>comparingDouble(activity -> score(session, activity))
                        .thenComparing(StravaActivitySummary::getStartDate)
                        .thenComparing(StravaActivitySummary::getId));
    }

    private double score(PlannedSessionResult session, StravaActivitySummary activity) {
        return dateDifference(session, activity)
                + relativeDifference(session.distanceKilometers() * 1000, activity.getDistanceMeters())
                + relativeDifference(session.durationSeconds(), activity.getMovingTimeSeconds());
    }

    private long dateDifference(PlannedSessionResult session, StravaActivitySummary activity) {
        return Math.abs(ChronoUnit.DAYS.between(
                session.scheduledDate(),
                activity.getStartDate().atZone(ZoneOffset.UTC).toLocalDate()));
    }

    private double relativeDifference(double planned, double actual) {
        return Math.abs(actual - planned) / planned;
    }
}
