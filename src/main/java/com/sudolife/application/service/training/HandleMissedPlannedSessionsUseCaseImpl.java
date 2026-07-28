package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.training.ports.provided.AdaptNextPlannedSessionUseCase;
import com.sudolife.application.service.training.ports.provided.HandleMissedPlannedSessionsUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class HandleMissedPlannedSessionsUseCaseImpl implements HandleMissedPlannedSessionsUseCase {

    private final AdaptiveRunningPlanRepository planRepository;
    private final AdaptNextPlannedSessionUseCase adaptNextPlannedSessionUseCase;
    private final TimeProvider timeProvider;
    private final Duration gracePeriod;

    public HandleMissedPlannedSessionsUseCaseImpl(
            AdaptiveRunningPlanRepository planRepository,
            AdaptNextPlannedSessionUseCase adaptNextPlannedSessionUseCase,
            TimeProvider timeProvider,
            @Value("${adaptive-coaching.missed-session-grace-period:24h}") Duration gracePeriod
    ) {
        this.planRepository = planRepository;
        this.adaptNextPlannedSessionUseCase = adaptNextPlannedSessionUseCase;
        this.timeProvider = timeProvider;
        this.gracePeriod = gracePeriod;
    }

    @Override
    @Transactional
    public void execute() {
        planRepository.findLatestPlans().forEach(this::handleMissedSessions);
    }

    private void handleMissedSessions(AdaptiveRunningPlan plan) {
        Instant now = timeProvider.now();
        List<AdaptiveRunningPlanSession> missedSessions = plan.getPlannedSessions().stream()
                .filter(session -> session.getStatus() == PlannedSessionStatus.PLANNED)
                .filter(session -> gracePeriodPassed(session, now))
                .toList();

        if (missedSessions.isEmpty()) {
            return;
        }

        missedSessions.forEach(AdaptiveRunningPlanSession::markMissed);
        planRepository.save(plan);

        if (hasUpcomingSession(plan, now)) {
            adaptNextPlannedSessionUseCase.execute(plan.getUserEmail(),
                    new AdaptNextPlannedSessionCommand(AdaptationTrigger.MISSED_PLANNED_SESSION));
        }
    }

    private boolean gracePeriodPassed(AdaptiveRunningPlanSession session, Instant now) {
        Instant graceDeadline = session.getPlannedSession().scheduledDate()
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .plus(gracePeriod);

        return now.isAfter(graceDeadline);
    }

    private boolean hasUpcomingSession(AdaptiveRunningPlan plan, Instant now) {
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();

        return plan.getPlannedSessions().stream()
                .filter(session -> session.getStatus() == PlannedSessionStatus.PLANNED)
                .anyMatch(session -> !session.getPlannedSession().scheduledDate().isBefore(today));
    }
}
