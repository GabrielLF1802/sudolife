package com.sudolife.application.service.ratelimit.ports.required;

import com.sudolife.application.service.ratelimit.RateLimitBucketKey;
import com.sudolife.application.service.ratelimit.RateLimitConsumption;

public interface RateLimitBucketRegistry {

    long availableTokens(RateLimitBucketKey key);

    RateLimitConsumption consume(RateLimitBucketKey key);

    void clear(RateLimitBucketKey key);
}
