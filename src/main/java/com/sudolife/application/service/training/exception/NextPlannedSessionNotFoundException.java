package com.sudolife.application.service.training.exception;

public class NextPlannedSessionNotFoundException extends RuntimeException {

    public NextPlannedSessionNotFoundException() {
        super("No next planned session is available for adaptation");
    }
}
