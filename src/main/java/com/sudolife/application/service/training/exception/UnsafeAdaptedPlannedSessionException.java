package com.sudolife.application.service.training.exception;

public class UnsafeAdaptedPlannedSessionException extends RuntimeException {

    public UnsafeAdaptedPlannedSessionException() {
        super("The adapted planned session is outside the safe training limits");
    }
}
