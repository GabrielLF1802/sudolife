package com.sudolife.application.service.ratelimit;

public record RateLimitBucketKey(RateLimitPolicy policy, String value) {

    public RateLimitBucketKey {
        if (policy == null) {
            throw new IllegalArgumentException("Rate limit policy is required");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Rate limit bucket key value is required");
        }

        value = value.trim();
    }
}
