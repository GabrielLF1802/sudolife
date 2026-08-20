package com.sudolife.application.service.ratelimit;

import java.time.Duration;

public record RateLimitPolicyProperties(boolean enabled, long capacity, Duration refillPeriod) {

    public RateLimitPolicyProperties {
        if (enabled && capacity < 1) {
            throw new IllegalArgumentException("Enabled rate limit capacity must be greater than zero");
        }
        if (enabled && (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative())) {
            throw new IllegalArgumentException("Enabled rate limit refill period must be positive");
        }
    }
}
