package com.sudolife.application.service.strava.exception;

public class DuplicateStravaAthleteOwnershipException extends StravaAccountLinkingException {

    public static final String FAILURE_CODE = "ATHLETE_ALREADY_LINKED";

    public DuplicateStravaAthleteOwnershipException() {
        super("Strava athlete is already linked to another user", FAILURE_CODE);
    }
}
