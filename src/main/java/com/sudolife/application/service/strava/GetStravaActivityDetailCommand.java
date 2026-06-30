package com.sudolife.application.service.strava;

public record GetStravaActivityDetailCommand(String userEmail, Long activityId) {

    public GetStravaActivityDetailCommand {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User email is required");
        }

        if (activityId == null || activityId <= 0) {
            throw new IllegalArgumentException("Activity id is invalid");
        }
    }
}
