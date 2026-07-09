package com.sudolife.application.service.training.exception;

public class InvalidTrainingProfileException extends RuntimeException {

    public InvalidTrainingProfileException(String message) {
        super(message);
    }

    public String getFailureCode() {
        return "INVALID_TRAINING_PROFILE";
    }
}
