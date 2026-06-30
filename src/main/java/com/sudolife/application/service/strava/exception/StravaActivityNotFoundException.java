package com.sudolife.application.service.strava.exception;

public class StravaActivityNotFoundException extends RuntimeException {

    public StravaActivityNotFoundException() {
        super("Strava activity not found");
    }
}
