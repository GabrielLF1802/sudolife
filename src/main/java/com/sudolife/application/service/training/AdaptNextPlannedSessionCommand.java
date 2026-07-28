package com.sudolife.application.service.training;

public record AdaptNextPlannedSessionCommand(AdaptationTrigger trigger) {

    public AdaptNextPlannedSessionCommand {
        if (trigger == null) {
            throw new IllegalArgumentException("Adaptation trigger is required");
        }
    }
}
