package com.sudolife.application.service.training;

public record CorrectPlannedSessionMatchCommand(Long plannedSessionId, Long activityId) {

    public CorrectPlannedSessionMatchCommand {
        if (plannedSessionId == null) {
            throw new IllegalArgumentException("Planned session id is required");
        }

        if (activityId == null) {
            throw new IllegalArgumentException("Activity id is required");
        }
    }
}
