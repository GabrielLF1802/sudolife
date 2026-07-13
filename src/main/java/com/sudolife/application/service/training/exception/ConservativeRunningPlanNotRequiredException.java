package com.sudolife.application.service.training.exception;

public class ConservativeRunningPlanNotRequiredException extends RuntimeException {

    public ConservativeRunningPlanNotRequiredException() {
        super("Running history and readiness do not require a conservative plan");
    }
}
