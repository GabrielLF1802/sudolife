package com.sudolife.application.service.strava.exception;

public class StravaAuthorizationFailureException extends StravaAccountLinkingException {

    public static final String FAILURE_CODE = "TOKEN_EXCHANGE_FAILED";

    public StravaAuthorizationFailureException() {
        super("Strava authorization could not be completed", FAILURE_CODE);
    }
}
