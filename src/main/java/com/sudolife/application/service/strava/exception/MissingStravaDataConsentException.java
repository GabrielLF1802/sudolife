package com.sudolife.application.service.strava.exception;

public class MissingStravaDataConsentException extends RuntimeException {

    public MissingStravaDataConsentException() {
        super("Current Strava data consent is required before OAuth starts");
    }
}
