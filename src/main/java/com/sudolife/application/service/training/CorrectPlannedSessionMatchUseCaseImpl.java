package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.service.strava.exception.StravaActivityNotFoundException;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.training.exception.AdaptiveRunningPlanNotFoundException;
import com.sudolife.application.service.training.ports.provided.CorrectPlannedSessionMatchUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CorrectPlannedSessionMatchUseCaseImpl implements CorrectPlannedSessionMatchUseCase {

    private final AdaptiveRunningPlanRepository planRepository;
    private final StravaActivitySummaryRepository activityRepository;
    private final PlannedSessionMatchPolicy matchPolicy = new PlannedSessionMatchPolicy();

    @Override
    @Transactional
    public CurrentAdaptiveRunningPlanResult execute(String userEmail, CorrectPlannedSessionMatchCommand command) {
        AdaptiveRunningPlan plan = planRepository.findLatestByUserEmail(userEmail)
                .orElseThrow(AdaptiveRunningPlanNotFoundException::new);
        AdaptiveRunningPlanSession session = plan.findPlannedSession(command.plannedSessionId());
        StravaActivitySummary activity = activityRepository.findByIdAndUserEmail(command.activityId(), userEmail)
                .orElseThrow(StravaActivityNotFoundException::new);
        validateAvailable(plan, session, activity);

        if (session.getMatchedActivityId() != null) {
            session.unlinkMatch();
        }

        session.match(activity.getId());

        return CurrentAdaptiveRunningPlanResult.from(planRepository.save(plan));
    }

    private void validateAvailable(
            AdaptiveRunningPlan plan,
            AdaptiveRunningPlanSession targetSession,
            StravaActivitySummary activity
    ) {
        if (!matchPolicy.matches(targetSession.getPlannedSession(), activity)) {
            throw new IllegalArgumentException("Activity is not eligible for the planned session");
        }

        boolean alreadyMatched = plan.getPlannedSessions().stream()
                .filter(session -> session != targetSession)
                .anyMatch(session -> activity.getId().equals(session.getMatchedActivityId()));

        if (alreadyMatched) {
            throw new IllegalArgumentException("Activity is already matched to another planned session");
        }
    }
}
