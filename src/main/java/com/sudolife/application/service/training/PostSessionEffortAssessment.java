package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import org.springframework.stereotype.Service;

@Service
public class PostSessionEffortAssessment {

    private static final double INFERENCE_TOLERANCE = 0.15;
    private static final double METERS_PER_KILOMETER = 1000.0;

    public AdaptationTrigger assess(
            AdaptiveRunningPlanSession completedSession,
            StravaActivitySummary matchedActivity
    ) {
        Integer reportedEffort = completedSession.getPostSessionPerceivedEffort();
        PlannedSessionTargetResult target = completedSession.getPlannedSession().target();

        if (reportedEffort != null) {
            return assessReported(reportedEffort, target);
        }

        return assessInferred(completedSession.getPlannedSession(), target, matchedActivity);
    }

    private AdaptationTrigger assessReported(int reportedEffort, PlannedSessionTargetResult target) {
        if (target.minimumPerceivedEffort() == null || target.maximumPerceivedEffort() == null) {
            return AdaptationTrigger.COMPLETED_PLANNED_SESSION;
        }

        if (reportedEffort > target.maximumPerceivedEffort()) {
            return AdaptationTrigger.UNEXPECTEDLY_HIGH_EFFORT;
        }

        if (reportedEffort < target.minimumPerceivedEffort()) {
            return AdaptationTrigger.UNEXPECTEDLY_LOW_EFFORT;
        }

        return AdaptationTrigger.COMPLETED_PLANNED_SESSION;
    }

    private AdaptationTrigger assessInferred(
            PlannedSessionResult plannedSession,
            PlannedSessionTargetResult target,
            StravaActivitySummary activity
    ) {
        if (activity == null) {
            return AdaptationTrigger.COMPLETED_PLANNED_SESSION;
        }

        AdaptationTrigger heartRateAssessment = assessHeartRate(target, activity.getAverageHeartRate());
        if (heartRateAssessment != AdaptationTrigger.COMPLETED_PLANNED_SESSION) {
            return heartRateAssessment;
        }

        return assessNormalizedDuration(plannedSession, activity);
    }

    private AdaptationTrigger assessHeartRate(PlannedSessionTargetResult target, Double averageHeartRate) {
        if (averageHeartRate == null || target.minimumHeartRate() == null || target.maximumHeartRate() == null) {
            return AdaptationTrigger.COMPLETED_PLANNED_SESSION;
        }

        if (averageHeartRate > target.maximumHeartRate()) {
            return AdaptationTrigger.UNEXPECTEDLY_HIGH_EFFORT;
        }

        if (averageHeartRate < target.minimumHeartRate()) {
            return AdaptationTrigger.UNEXPECTEDLY_LOW_EFFORT;
        }

        return AdaptationTrigger.COMPLETED_PLANNED_SESSION;
    }

    private AdaptationTrigger assessNormalizedDuration(
            PlannedSessionResult plannedSession,
            StravaActivitySummary activity
    ) {
        if (plannedSession.durationSeconds() == null || activity.getMovingTimeSeconds() == null
                || activity.getDistanceMeters() == null || activity.getDistanceMeters() <= 0) {
            return AdaptationTrigger.COMPLETED_PLANNED_SESSION;
        }

        double actualDistanceKilometers = activity.getDistanceMeters() / METERS_PER_KILOMETER;
        double normalizedDuration = activity.getMovingTimeSeconds()
                * plannedSession.distanceKilometers() / actualDistanceKilometers;

        if (normalizedDuration > plannedSession.durationSeconds() * (1 + INFERENCE_TOLERANCE)) {
            return AdaptationTrigger.UNEXPECTEDLY_HIGH_EFFORT;
        }

        if (normalizedDuration < plannedSession.durationSeconds() * (1 - INFERENCE_TOLERANCE)) {
            return AdaptationTrigger.UNEXPECTEDLY_LOW_EFFORT;
        }

        return AdaptationTrigger.COMPLETED_PLANNED_SESSION;
    }
}
