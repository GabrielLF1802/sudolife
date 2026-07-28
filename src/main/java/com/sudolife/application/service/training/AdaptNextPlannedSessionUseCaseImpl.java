package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.training.exception.AdaptiveRunningPlanNotFoundException;
import com.sudolife.application.service.training.exception.NextPlannedSessionNotFoundException;
import com.sudolife.application.service.training.ports.provided.AdaptNextPlannedSessionUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AdaptNextPlannedSessionUseCaseImpl implements AdaptNextPlannedSessionUseCase {

    private final AdaptiveRunningPlanRepository adaptiveRunningPlanRepository;
    private final CoachingProfileRepository coachingProfileRepository;
    private final StravaActivitySummaryRepository activityRepository;
    private final PostSessionEffortAssessment effortAssessment;
    private final AdaptedPlannedSessionValidator validator;
    private final TimeProvider timeProvider;

    @Override
    public CurrentAdaptiveRunningPlanResult execute(String userEmail, AdaptNextPlannedSessionCommand command) {
        AdaptiveRunningPlan plan = adaptiveRunningPlanRepository.findLatestByUserEmail(userEmail)
                .orElseThrow(AdaptiveRunningPlanNotFoundException::new);
        AdaptiveRunningPlanSession nextSession = nextSession(plan);
        AdaptationTrigger effortTrigger = effortTrigger(plan, userEmail, command.trigger());
        AdaptationTrigger trigger = effectiveTrigger(userEmail, effortTrigger);
        PlannedSessionResult replacement = replacement(nextSession.getPlannedSession(), trigger);
        validator.validate(nextSession.getPlannedSession(), replacement);
        plan.replacePlannedSession(nextSession.getId(), replacement, trigger);

        return CurrentAdaptiveRunningPlanResult.from(adaptiveRunningPlanRepository.save(plan));
    }

    private AdaptationTrigger effortTrigger(
            AdaptiveRunningPlan plan,
            String userEmail,
            AdaptationTrigger requestedTrigger
    ) {
        if (requestedTrigger != AdaptationTrigger.COMPLETED_PLANNED_SESSION) {
            return requestedTrigger;
        }

        AdaptiveRunningPlanSession completedSession = plan.getPlannedSessions().stream()
                .filter(session -> session.getStatus() == PlannedSessionStatus.COMPLETED)
                .max(Comparator.comparing(session -> session.getPlannedSession().scheduledDate()))
                .orElse(null);
        if (completedSession == null) {
            return requestedTrigger;
        }

        StravaActivitySummary activity = completedSession.getMatchedActivityId() == null
                ? null
                : activityRepository.findByIdAndUserEmail(completedSession.getMatchedActivityId(), userEmail)
                        .orElse(null);

        return effortAssessment.assess(completedSession, activity);
    }

    private AdaptationTrigger effectiveTrigger(String userEmail, AdaptationTrigger requestedTrigger) {
        return coachingProfileRepository.findByUserEmail(userEmail)
                .filter(profile -> profile.isInjuryConcern())
                .map(profile -> AdaptationTrigger.INJURY_CONCERN)
                .orElse(requestedTrigger);
    }

    private AdaptiveRunningPlanSession nextSession(AdaptiveRunningPlan plan) {
        LocalDate today = timeProvider.now().atZone(ZoneOffset.UTC).toLocalDate();

        return plan.getPlannedSessions().stream()
                .filter(session -> session.getStatus() == PlannedSessionStatus.PLANNED)
                .filter(session -> !session.getPlannedSession().scheduledDate().isBefore(today))
                .min(Comparator.comparing(session -> session.getPlannedSession().scheduledDate()))
                .orElseThrow(NextPlannedSessionNotFoundException::new);
    }

    private PlannedSessionResult replacement(PlannedSessionResult original, AdaptationTrigger trigger) {
        double multiplier = switch (trigger) {
            case COMPLETED_PLANNED_SESSION, UNEXPECTEDLY_LOW_EFFORT -> 1.05;
            case MISSED_PLANNED_SESSION, LOW_READINESS, UNEXPECTEDLY_HIGH_EFFORT,
                    INJURY_CONCERN_CLEARED -> 0.70;
            case INJURY_CONCERN -> 0;
        };
        PlannedSessionType type = trigger == AdaptationTrigger.INJURY_CONCERN
                ? PlannedSessionType.RECOVERY : original.type();
        PlannedSessionTargetResult target = trigger == AdaptationTrigger.INJURY_CONCERN
                ? PlannedSessionTargetResult.perceivedEffort(1, 3) : original.target();

        return new PlannedSessionResult(
                original.weekNumber(),
                original.sessionNumber(),
                type,
                Math.round(original.distanceKilometers() * multiplier * 10.0) / 10.0,
                target,
                original.scheduledDate(),
                original.durationSeconds() == null
                        ? null : (int) Math.round(original.durationSeconds() * multiplier));
    }
}
