package com.sudolife.application.service.training;

public record SubmitPostSessionPerceivedEffortCommand(Long plannedSessionId, int perceivedEffort) {

    public SubmitPostSessionPerceivedEffortCommand {
        if (plannedSessionId == null) {
            throw new IllegalArgumentException("Planned session id is required");
        }

        if (perceivedEffort < 1 || perceivedEffort > 10) {
            throw new IllegalArgumentException("Perceived effort must be between 1 and 10");
        }
    }
}
