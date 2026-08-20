package com.sudolife.adapter.driving.rest.ratelimit;

public record RateLimitBucketKey(RestRateLimitPolicy policy, String value) {

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
