package com.sudolife.adapter.driving.rest.ratelimit;

public class LoginRateLimitExceededException extends RuntimeException {

    public LoginRateLimitExceededException() {
        super("Login rate limit exceeded");
    }
}
