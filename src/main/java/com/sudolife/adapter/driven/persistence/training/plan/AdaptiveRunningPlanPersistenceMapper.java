package com.sudolife.adapter.driven.persistence.training.plan;

import com.sudolife.adapter.driven.persistence.training.plan.entitymodel.AdaptiveRunningPlanEntity;
import com.sudolife.adapter.driven.persistence.training.plan.entitymodel.AdaptiveRunningPlanSessionEntity;
import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.model.training.AdaptiveRunningPlanSession;
import com.sudolife.application.model.training.RunningGoal;
import com.sudolife.application.service.training.PlannedSessionResult;
import com.sudolife.application.service.training.PlannedSessionStatus;
import com.sudolife.application.service.training.PlannedSessionTargetResult;
import com.sudolife.application.service.training.PlannedSessionTargetType;
import com.sudolife.application.service.training.PlannedSessionType;
import org.springframework.stereotype.Component;

@Component
public class AdaptiveRunningPlanPersistenceMapper {

    public AdaptiveRunningPlanEntity toEntity(AdaptiveRunningPlan plan) {
        AdaptiveRunningPlanEntity entity = new AdaptiveRunningPlanEntity();
        entity.setId(plan.getId());
        entity.setUserEmail(plan.getUserEmail());
        entity.setSafeMilestoneDistanceKilometers(plan.getSafeMilestone().getTargetDistanceKilometers());
        entity.setSafeMilestonePaceSecondsPerKilometer(plan.getSafeMilestone().getTargetPaceSecondsPerKilometer());
        entity.setSafeMilestoneTargetDate(plan.getSafeMilestone().getTargetDate());
        entity.setExplanation(plan.getExplanation());
        entity.setAcceptedAt(plan.getAcceptedAt());
        plan.getPlannedSessions().forEach(session -> entity.getPlannedSessions().add(toEntity(session, entity)));

        return entity;
    }

    public AdaptiveRunningPlan toDomain(AdaptiveRunningPlanEntity entity) {
        return new AdaptiveRunningPlan(
                entity.getId(),
                entity.getUserEmail(),
                new RunningGoal(
                        entity.getSafeMilestoneDistanceKilometers(),
                        entity.getSafeMilestonePaceSecondsPerKilometer(),
                        entity.getSafeMilestoneTargetDate()),
                entity.getExplanation(),
                entity.getAcceptedAt(),
                entity.getPlannedSessions().stream().map(this::toDomain).toList());
    }

    private AdaptiveRunningPlanSessionEntity toEntity(
            AdaptiveRunningPlanSession session,
            AdaptiveRunningPlanEntity plan
    ) {
        PlannedSessionResult plannedSession = session.getPlannedSession();
        PlannedSessionTargetResult target = plannedSession.target();
        AdaptiveRunningPlanSessionEntity entity = new AdaptiveRunningPlanSessionEntity();
        entity.setId(session.getId());
        entity.setPlan(plan);
        entity.setOriginalPlannedSessionId(session.getOriginalPlannedSessionId());
        entity.setWeekNumber(plannedSession.weekNumber());
        entity.setSessionNumber(plannedSession.sessionNumber());
        entity.setSessionType(plannedSession.type().name());
        entity.setDistanceKilometers(plannedSession.distanceKilometers());
        entity.setTargetType(target.type().name());
        entity.setMinimumHeartRate(target.minimumHeartRate());
        entity.setMaximumHeartRate(target.maximumHeartRate());
        entity.setMinimumPerceivedEffort(target.minimumPerceivedEffort());
        entity.setMaximumPerceivedEffort(target.maximumPerceivedEffort());
        entity.setScheduledDate(plannedSession.scheduledDate());
        entity.setStatus(session.getStatus().name());

        return entity;
    }

    private AdaptiveRunningPlanSession toDomain(AdaptiveRunningPlanSessionEntity entity) {
        PlannedSessionTargetType targetType = PlannedSessionTargetType.valueOf(entity.getTargetType());
        PlannedSessionTargetResult target = targetType == PlannedSessionTargetType.HEART_RATE
                ? PlannedSessionTargetResult.heartRate(entity.getMinimumHeartRate(), entity.getMaximumHeartRate())
                : PlannedSessionTargetResult.perceivedEffort(
                        entity.getMinimumPerceivedEffort(), entity.getMaximumPerceivedEffort());
        PlannedSessionResult plannedSession = new PlannedSessionResult(
                entity.getWeekNumber(),
                entity.getSessionNumber(),
                PlannedSessionType.valueOf(entity.getSessionType()),
                entity.getDistanceKilometers(),
                target,
                entity.getScheduledDate());

        return new AdaptiveRunningPlanSession(
                entity.getId(),
                entity.getOriginalPlannedSessionId(),
                plannedSession,
                PlannedSessionStatus.valueOf(entity.getStatus()));
    }
}
