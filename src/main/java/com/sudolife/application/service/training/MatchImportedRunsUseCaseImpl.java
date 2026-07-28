package com.sudolife.application.service.training;

import com.sudolife.application.model.strava.StravaActivitySummary;
import com.sudolife.application.model.strava.StravaActivityType;
import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.service.strava.ports.required.StravaActivitySummaryRepository;
import com.sudolife.application.service.training.ports.provided.MatchImportedRunsUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchImportedRunsUseCaseImpl implements MatchImportedRunsUseCase {

    private final AdaptiveRunningPlanRepository planRepository;
    private final StravaActivitySummaryRepository activityRepository;
    private final PlannedSessionMatchPolicy matchPolicy = new PlannedSessionMatchPolicy();

    @Override
    @Transactional
    public void execute(String userEmail) {
        planRepository.findLatestByUserEmail(userEmail).ifPresent(this::matchPlan);
    }

    private void matchPlan(AdaptiveRunningPlan plan) {
        Set<Long> matchedActivityIds = new HashSet<>();
        plan.getPlannedSessions().stream()
                .map(AdaptiveRunningPlanSession::getMatchedActivityId)
                .filter(java.util.Objects::nonNull)
                .forEach(matchedActivityIds::add);
        boolean changed = false;

        for (AdaptiveRunningPlanSession session : matchableSessions(plan)) {
            List<StravaActivitySummary> candidates = candidates(plan.getUserEmail(), session).stream()
                    .filter(activity -> !matchedActivityIds.contains(activity.getId()))
                    .toList();
            java.util.Optional<StravaActivitySummary> match = matchPolicy.closestCandidate(
                    session.getPlannedSession(), candidates);

            if (match.isPresent()) {
                session.match(match.get().getId());
                matchedActivityIds.add(match.get().getId());
                changed = true;
            }
        }

        if (changed) {
            planRepository.save(plan);
        }
    }

    private List<AdaptiveRunningPlanSession> matchableSessions(AdaptiveRunningPlan plan) {
        return plan.getPlannedSessions().stream()
                .filter(session -> session.getStatus() == PlannedSessionStatus.PLANNED)
                .sorted(Comparator.comparing((AdaptiveRunningPlanSession session) ->
                                session.getPlannedSession().scheduledDate())
                        .thenComparing(AdaptiveRunningPlanSession::getId))
                .toList();
    }

    private List<StravaActivitySummary> candidates(String userEmail, AdaptiveRunningPlanSession session) {
        java.time.LocalDate scheduledDate = session.getPlannedSession().scheduledDate();

        return activityRepository.findByUserEmailAndActivityTypeAndStartDateBetween(
                userEmail,
                StravaActivityType.RUN,
                scheduledDate.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                scheduledDate.plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC));
    }
}
