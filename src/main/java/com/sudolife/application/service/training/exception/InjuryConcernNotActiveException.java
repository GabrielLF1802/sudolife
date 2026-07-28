package com.sudolife.application.service.training.exception;

public class InjuryConcernNotActiveException extends RuntimeException {

    public InjuryConcernNotActiveException() {
        super("Injury concern is not active");
    }
}
