package com.sudolife.adapter.driving.rest.ratelimit;

import java.time.Duration;

public record RateLimitConsumption(boolean allowed, long remainingTokens, Duration retryAfter) {

    public static RateLimitConsumption allowed(long remainingTokens) {
        return new RateLimitConsumption(true, remainingTokens, Duration.ZERO);
    }

    public static RateLimitConsumption allowedWithoutLimit() {
        return new RateLimitConsumption(true, Long.MAX_VALUE, Duration.ZERO);
    }

    public static RateLimitConsumption blocked(long remainingTokens, Duration retryAfter) {
        return new RateLimitConsumption(false, remainingTokens, retryAfter);
    }
}
