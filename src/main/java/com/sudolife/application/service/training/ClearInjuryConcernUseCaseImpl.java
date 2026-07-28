package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.model.training.UserReportedReadiness;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.exception.AdaptiveRunningPlanNotFoundException;
import com.sudolife.application.service.training.exception.CoachingProfileRequiredException;
import com.sudolife.application.service.training.exception.InjuryConcernNotActiveException;
import com.sudolife.application.service.training.exception.InvalidCoachingProfileException;
import com.sudolife.application.service.training.exception.NextPlannedSessionNotFoundException;
import com.sudolife.application.service.training.ports.provided.ClearInjuryConcernUseCase;
import com.sudolife.application.service.training.ports.provided.GetRunningHistorySnapshotUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class ClearInjuryConcernUseCaseImpl implements ClearInjuryConcernUseCase {

    private static final double MODERATE_RESUME_FACTOR = 0.75;
    private static final double LOW_READINESS_RESUME_FACTOR = 0.50;
    private static final double DEFAULT_DISTANCE_KILOMETERS = 2.0;
    private static final double MAXIMUM_RESUME_DISTANCE_KILOMETERS = 3.0;
    private static final double DEFAULT_PACE_SECONDS_PER_KILOMETER = 360.0;

    private final CoachingProfileRepository coachingProfileRepository;
    private final AdaptiveRunningPlanRepository adaptiveRunningPlanRepository;
    private final GetRunningHistorySnapshotUseCase runningHistoryUseCase;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public CurrentAdaptiveRunningPlanResult execute(String userEmail, ClearInjuryConcernCommand command) {
        CoachingProfile profile = activeInjuryProfile(userEmail);
        UserReportedReadiness readiness = readiness(command);
        RunningHistorySnapshotResult history = runningHistoryUseCase.execute(userEmail);
        AdaptiveRunningPlan plan = adaptiveRunningPlanRepository.findLatestByUserEmail(userEmail)
                .orElseThrow(AdaptiveRunningPlanNotFoundException::new);
        AdaptiveRunningPlanSession nextSession = nextSession(plan);
        PlannedSessionResult replacement = conservativeReplacement(nextSession.getPlannedSession(), history, readiness);

        coachingProfileRepository.save(clearedProfile(profile, readiness));
        plan.replacePlannedSession(
                nextSession.getId(), replacement, AdaptationTrigger.INJURY_CONCERN_CLEARED);

        return CurrentAdaptiveRunningPlanResult.from(adaptiveRunningPlanRepository.save(plan));
    }

    private CoachingProfile activeInjuryProfile(String userEmail) {
        CoachingProfile profile = coachingProfileRepository.findByUserEmail(userEmail)
                .orElseThrow(CoachingProfileRequiredException::new);

        if (!profile.isInjuryConcern()) {
            throw new InjuryConcernNotActiveException();
        }

        return profile;
    }

    private UserReportedReadiness readiness(ClearInjuryConcernCommand command) {
        try {
            return UserReportedReadiness.from(command == null ? null : command.readiness());
        } catch (IllegalArgumentException exception) {
            throw new InvalidCoachingProfileException(exception.getMessage());
        }
    }

    private CoachingProfile clearedProfile(CoachingProfile profile, UserReportedReadiness readiness) {
        return new CoachingProfile(
                profile.getId(),
                profile.getUserEmail(),
                profile.getRunningGoal(),
                readiness,
                false,
                profile.getRunningAvailability());
    }

    private AdaptiveRunningPlanSession nextSession(AdaptiveRunningPlan plan) {
        LocalDate today = timeProvider.now().atZone(ZoneOffset.UTC).toLocalDate();

        return plan.getPlannedSessions().stream()
                .filter(session -> session.getStatus() == PlannedSessionStatus.PLANNED)
                .filter(session -> !session.getPlannedSession().scheduledDate().isBefore(today))
                .min(Comparator.comparing(session -> session.getPlannedSession().scheduledDate()))
                .orElseThrow(NextPlannedSessionNotFoundException::new);
    }

    private PlannedSessionResult conservativeReplacement(
            PlannedSessionResult original,
            RunningHistorySnapshotResult history,
            UserReportedReadiness readiness
    ) {
        double resumeFactor = readiness == UserReportedReadiness.LOW
                ? LOW_READINESS_RESUME_FACTOR : MODERATE_RESUME_FACTOR;
        double averageDistance = history.runningActivityCount() == 0
                ? DEFAULT_DISTANCE_KILOMETERS
                : history.totalDistanceKilometers() / history.runningActivityCount();
        double distance = rounded(Math.min(MAXIMUM_RESUME_DISTANCE_KILOMETERS, averageDistance * resumeFactor));
        double pace = history.representativePaceSecondsPerKilometer() == null
                ? DEFAULT_PACE_SECONDS_PER_KILOMETER : history.representativePaceSecondsPerKilometer();

        return new PlannedSessionResult(
                original.weekNumber(),
                original.sessionNumber(),
                PlannedSessionType.EASY_RUN,
                distance,
                PlannedSessionTargetResult.perceivedEffort(1, 3),
                original.scheduledDate(),
                (int) Math.round(distance * pace));
    }

    private double rounded(double distance) {
        return Math.round(distance * 10.0) / 10.0;
    }
}
