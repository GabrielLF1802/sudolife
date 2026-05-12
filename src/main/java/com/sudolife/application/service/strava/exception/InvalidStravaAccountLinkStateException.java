package com.sudolife.application.service.strava.exception;

public class InvalidStravaAccountLinkStateException extends StravaAccountLinkingException {

    public static final String FAILURE_CODE = "INVALID_LINK_STATE";

    public InvalidStravaAccountLinkStateException() {
        super("Strava account link state is invalid", FAILURE_CODE);
    }
}
