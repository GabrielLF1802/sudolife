package com.sudolife.application.service.ratelimit.exception;

public class RegisterRateLimitExceededException extends RuntimeException {

    public RegisterRateLimitExceededException() {
        super("Register rate limit exceeded");
    }
}
