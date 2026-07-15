package com.sudolife.application.service.training.exception;

public class UnsafeAiRunningPlanException extends RuntimeException {

    public UnsafeAiRunningPlanException() {
        super("The AI running plan proposal is unsafe");
    }
}
