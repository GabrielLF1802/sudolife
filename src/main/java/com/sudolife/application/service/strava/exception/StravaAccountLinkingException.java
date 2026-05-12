package com.sudolife.application.service.strava.exception;

public abstract class StravaAccountLinkingException extends RuntimeException {

    private final String failureCode;

    protected StravaAccountLinkingException(String message, String failureCode) {
        super(message);

        this.failureCode = failureCode;
    }

    public String getFailureCode() {
        return failureCode;
    }
}
