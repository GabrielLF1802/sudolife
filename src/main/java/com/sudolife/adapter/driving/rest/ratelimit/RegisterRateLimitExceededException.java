package com.sudolife.adapter.driving.rest.ratelimit;

public class RegisterRateLimitExceededException extends RuntimeException {

    public RegisterRateLimitExceededException() {
        super("Register rate limit exceeded");
    }
}
