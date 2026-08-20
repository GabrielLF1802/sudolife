package com.sudolife.application.service.ratelimit.exception;

public class LoginRateLimitExceededException extends RuntimeException {

    public LoginRateLimitExceededException() {
        super("Login rate limit exceeded");
    }
}
