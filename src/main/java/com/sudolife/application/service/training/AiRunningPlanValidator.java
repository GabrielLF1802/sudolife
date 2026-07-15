package com.sudolife.application.service.training;

import com.sudolife.application.service.training.exception.UnsafeAiRunningPlanException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiRunningPlanValidator {

    private static final double MAXIMUM_DISTANCE_MULTIPLIER = 1.10;

    public ValidatedAiRunningPlan validate(TrainingSnapshot snapshot, AiRunningPlanProposal proposal) {
        List<PlannedSessionResult> safeSessions = snapshot.safeSessions();

        if (proposal.plannedSessions().size() != safeSessions.size()) {
            throw new UnsafeAiRunningPlanException();
        }

        boolean adjusted = false;
        for (int index = 0; index < safeSessions.size(); index++) {
            PlannedSessionResult proposed = proposal.plannedSessions().get(index);
            PlannedSessionResult safe = safeSessions.get(index);

            if (proposed.weekNumber() != safe.weekNumber()
                    || proposed.sessionNumber() != safe.sessionNumber()
                    || proposed.type() != safe.type()
                    || !proposed.scheduledDate().equals(safe.scheduledDate())
                    || proposed.distanceKilometers() > safe.distanceKilometers() * MAXIMUM_DISTANCE_MULTIPLIER) {
                throw new UnsafeAiRunningPlanException();
            }

            adjusted |= proposed.distanceKilometers() != safe.distanceKilometers()
                    || !proposed.target().equals(safe.target());
        }

        return new ValidatedAiRunningPlan(safeSessions, adjusted);
    }
}
