package com.sudolife.adapter.driving.rest.ratelimit;

public interface RestRateLimitBucketRegistry {

    long availableTokens(RateLimitBucketKey key);

    RateLimitConsumption consume(RateLimitBucketKey key);

    void clear(RateLimitBucketKey key);
}
