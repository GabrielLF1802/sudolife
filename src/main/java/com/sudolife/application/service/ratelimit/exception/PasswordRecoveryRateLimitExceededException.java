package com.sudolife.application.service.ratelimit.exception;

public class PasswordRecoveryRateLimitExceededException extends RuntimeException {

    public PasswordRecoveryRateLimitExceededException() {
        super("Password recovery rate limit exceeded");
    }
}
