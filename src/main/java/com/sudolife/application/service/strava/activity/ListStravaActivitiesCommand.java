package com.sudolife.application.service.strava.activity;

public record ListStravaActivitiesCommand(String userEmail, int page, int size) {

    public ListStravaActivitiesCommand {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User email is invalid");
        }

        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
    }
}
