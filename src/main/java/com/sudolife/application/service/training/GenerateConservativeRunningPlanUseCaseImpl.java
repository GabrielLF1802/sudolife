package com.sudolife.application.service.training;

import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.training.exception.CoachingProfileRequiredException;
import com.sudolife.application.service.training.exception.ConservativeRunningPlanNotRequiredException;
import com.sudolife.application.service.training.ports.provided.GenerateConservativeRunningPlanUseCase;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class GenerateConservativeRunningPlanUseCaseImpl implements GenerateConservativeRunningPlanUseCase {

    private static final int DURATION_WEEKS = 4;
    private static final int SESSIONS_PER_WEEK = 2;
    private static final int STANDARD_PROGRESSION_PERCENT = 5;

    private final CoachingProfileRepository coachingProfileRepository;
    private final TrainingProfileRepository trainingProfileRepository;
    private final GetRunningHistorySnapshotUseCase runningHistoryUseCase;
    private final TimeProvider timeProvider;

    @Override
    public ConservativeRunningPlanResult execute(String userEmail) {
        CoachingProfile coachingProfile = coachingProfileRepository.findByUserEmail(userEmail)
                .orElseThrow(CoachingProfileRequiredException::new);
        RunningHistorySnapshotResult history = runningHistoryUseCase.execute(userEmail);

        if (coachingProfile.isInjuryConcern()) {
            return recoveryPlan(coachingProfile, recoveryTarget(userEmail));
        }

        List<ConservativeRunningPlanReason> reasons = reasons(coachingProfile, history);

        if (reasons.isEmpty()) {
            throw new ConservativeRunningPlanNotRequiredException();
        }

        boolean lowReadiness = coachingProfile.getReadiness() == UserReportedReadiness.LOW;
        int progressionPercent = lowReadiness ? 0 : STANDARD_PROGRESSION_PERCENT;
        double baseDistance = baseDistance(
                history, lowReadiness, coachingProfile.getTargetDistanceKilometers(), progressionPercent);
        PlannedSessionTargetResult target = target(userEmail, lowReadiness);

        return new ConservativeRunningPlanResult(
                ConservativeRunningPlanClassification.CONSERVATIVE,
                reasons,
                coachingProfile.getTargetDistanceKilometers(),
                DURATION_WEEKS,
                SESSIONS_PER_WEEK,
                progressionPercent,
                sessions(baseDistance, progressionPercent, target)
        );
    }

    private List<ConservativeRunningPlanReason> reasons(
            CoachingProfile coachingProfile,
            RunningHistorySnapshotResult history
    ) {
        List<ConservativeRunningPlanReason> reasons = new ArrayList<>();

        if (!history.sufficientRunningHistory()) {
            reasons.add(ConservativeRunningPlanReason.INSUFFICIENT_HISTORY);
        }

        if (coachingProfile.getReadiness() == UserReportedReadiness.LOW) {
            reasons.add(ConservativeRunningPlanReason.LOW_READINESS);
        }

        return List.copyOf(reasons);
    }

    private double baseDistance(
            RunningHistorySnapshotResult history,
            boolean lowReadiness,
            double longTermGoalDistance,
            int progressionPercent
    ) {
        double averageDistance = history.runningActivityCount() == 0
                ? 2.0
                : history.totalDistanceKilometers() / history.runningActivityCount();
        double maximumDistance = lowReadiness ? 3.0 : 5.0;
        double finalWeekProgression = Math.pow(1 + progressionPercent / 100.0, DURATION_WEEKS - 1);
        double goalBoundedDistance = longTermGoalDistance / (1.25 * finalWeekProgression);

        return Math.min(goalBoundedDistance, Math.min(maximumDistance, Math.max(2.0, averageDistance * 0.75)));
    }

    private PlannedSessionTargetResult target(String userEmail, boolean lowReadiness) {
        List<TrainingHeartRateZone> zones = coachingHeartRateZones(userEmail);

        if (zones.isEmpty()) {
            return PlannedSessionTargetResult.perceivedEffort(lowReadiness ? 3 : 4);
        }

        TrainingHeartRateZone firstZone = zones.getFirst();
        TrainingHeartRateZone upperEasyZone = zones.get(Math.min(1, zones.size() - 1));

        return PlannedSessionTargetResult.heartRate(
                firstZone.minimumHeartRate(), upperEasyZone.maximumHeartRate());
    }

    private PlannedSessionTargetResult recoveryTarget(String userEmail) {
        List<TrainingHeartRateZone> zones = coachingHeartRateZones(userEmail);

        if (zones.isEmpty()) {
            return PlannedSessionTargetResult.perceivedEffort(1, 3);
        }

        TrainingHeartRateZone firstZone = zones.getFirst();
        TrainingHeartRateZone upperEasyZone = zones.get(Math.min(1, zones.size() - 1));

        return PlannedSessionTargetResult.heartRate(
                firstZone.minimumHeartRate(), upperEasyZone.maximumHeartRate());
    }

    private List<TrainingHeartRateZone> coachingHeartRateZones(String userEmail) {
        int currentYear = timeProvider.now().atZone(ZoneOffset.UTC).getYear();

        return trainingProfileRepository.findByUserEmail(userEmail)
                .map(profile -> TrainingProfileResult.existing(
                        profile.getBirthYear(), profile.getImportedHeartRateZones(), currentYear).heartRateZones())
                .orElseGet(List::of);
    }

    private ConservativeRunningPlanResult recoveryPlan(
            CoachingProfile coachingProfile,
            PlannedSessionTargetResult target
    ) {
        return new ConservativeRunningPlanResult(
                ConservativeRunningPlanClassification.RECOVERY_ONLY,
                List.of(ConservativeRunningPlanReason.INJURY_CONCERN),
                coachingProfile.getTargetDistanceKilometers(),
                DURATION_WEEKS,
                SESSIONS_PER_WEEK,
                0,
                recoverySessions(target)
        );
    }

    private List<PlannedSessionResult> recoverySessions(PlannedSessionTargetResult target) {
        List<PlannedSessionResult> sessions = new ArrayList<>();

        for (int weekNumber = 1; weekNumber <= DURATION_WEEKS; weekNumber++) {
            for (int sessionNumber = 1; sessionNumber <= SESSIONS_PER_WEEK; sessionNumber++) {
                sessions.add(new PlannedSessionResult(
                        weekNumber, sessionNumber, PlannedSessionType.RECOVERY, 0, target));
            }
        }

        return List.copyOf(sessions);
    }

    private List<PlannedSessionResult> sessions(
            double baseDistance,
            int progressionPercent,
            PlannedSessionTargetResult target
    ) {
        List<PlannedSessionResult> sessions = new ArrayList<>();

        for (int weekNumber = 1; weekNumber <= DURATION_WEEKS; weekNumber++) {
            double progression = Math.pow(1 + progressionPercent / 100.0, weekNumber - 1);
            sessions.add(session(weekNumber, 1, PlannedSessionType.EASY_RUN, baseDistance * progression, target));
            sessions.add(session(weekNumber, 2, PlannedSessionType.LONG_RUN, baseDistance * 1.25 * progression, target));
        }

        return List.copyOf(sessions);
    }

    private PlannedSessionResult session(
            int weekNumber,
            int sessionNumber,
            PlannedSessionType type,
            double distanceKilometers,
            PlannedSessionTargetResult target
    ) {
        double roundedDistance = Math.round(distanceKilometers * 10.0) / 10.0;

        return new PlannedSessionResult(weekNumber, sessionNumber, type, roundedDistance, target);
    }
}
