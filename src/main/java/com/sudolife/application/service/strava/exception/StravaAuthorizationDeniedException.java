package com.sudolife.application.service.strava.exception;

public class StravaAuthorizationDeniedException extends StravaAccountLinkingException {

    public static final String FAILURE_CODE = "AUTHORIZATION_DENIED";

    public StravaAuthorizationDeniedException() {
        super("Strava authorization was denied", FAILURE_CODE);
    }
}
