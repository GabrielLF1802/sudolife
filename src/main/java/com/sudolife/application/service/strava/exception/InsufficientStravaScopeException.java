package com.sudolife.application.service.strava.exception;

public class InsufficientStravaScopeException extends StravaAccountLinkingException {

    public static final String FAILURE_CODE = "INSUFFICIENT_SCOPE";

    public InsufficientStravaScopeException() {
        super("Strava authorization scope is insufficient", FAILURE_CODE);
    }
}
