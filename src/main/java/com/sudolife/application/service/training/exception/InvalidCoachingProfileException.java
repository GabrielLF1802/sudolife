package com.sudolife.application.service.training.exception;

public class InvalidCoachingProfileException extends RuntimeException {

    public InvalidCoachingProfileException(String message) {
        super(message);
    }

    public String getFailureCode() {
        return "INVALID_COACHING_PROFILE";
    }
}
