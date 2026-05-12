package com.sudolife.application.service.strava.exception;

public class InvalidStravaAuthorizationStateException extends StravaAccountLinkingException {

    public static final String FAILURE_CODE = "INVALID_STATE";

    public InvalidStravaAuthorizationStateException() {
        super("Strava authorization state is invalid or expired", FAILURE_CODE);
    }
}
