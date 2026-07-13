package com.sudolife.application.service.training.exception;

public class CoachingProfileRequiredException extends RuntimeException {

    public CoachingProfileRequiredException() {
        super("A configured coaching profile is required");
    }
}
