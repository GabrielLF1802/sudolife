package com.sudolife.adapter.driven.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitPolicyProperties;
import com.sudolife.config.security.infraestructure.ratelimit.RestRateLimitProperties;
import com.sudolife.application.service.ratelimit.RateLimitBucketKey;
import com.sudolife.application.service.ratelimit.RateLimitConsumption;
import com.sudolife.application.service.ratelimit.ports.required.RateLimitBucketRegistry;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.TimeMeter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InProcessBucket4JRateLimitBucketRegistry implements RateLimitBucketRegistry {

    private final RestRateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentMap<RateLimitBucketKey, BucketEntry> buckets = new ConcurrentHashMap<>();

    @Autowired
    public InProcessBucket4JRateLimitBucketRegistry(RestRateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    InProcessBucket4JRateLimitBucketRegistry(RestRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public long availableTokens(RateLimitBucketKey key) {
        RateLimitPolicyProperties policy = properties.policy(key.policy());
        if (!policy.enabled()) {
            return Long.MAX_VALUE;
        }

        return resolveEntry(key, policy).bucket().getAvailableTokens();
    }

    @Override
    public RateLimitConsumption consume(RateLimitBucketKey key) {
        RateLimitPolicyProperties policy = properties.policy(key.policy());
        if (!policy.enabled()) {
            return RateLimitConsumption.allowedWithoutLimit();
        }

        ConsumptionProbe probe = resolveEntry(key, policy).bucket().tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitConsumption.allowed(probe.getRemainingTokens());
        }

        return RateLimitConsumption.blocked(probe.getRemainingTokens(), Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }

    @Override
    public void clear(RateLimitBucketKey key) {
        buckets.remove(key);
    }

    int storedBucketCount() {
        clearExpiredBuckets();

        return buckets.size();
    }

    private BucketEntry resolveEntry(RateLimitBucketKey key, RateLimitPolicyProperties policy) {
        clearExpiredBuckets();
        Instant expiresAt = clock.instant().plus(policy.refillPeriod());

        return buckets.compute(key, (bucketKey, existingEntry) -> {
            if (existingEntry == null || existingEntry.expiresAt().isBefore(clock.instant())
                    || existingEntry.expiresAt().equals(clock.instant())) {
                return new BucketEntry(createBucket(policy), expiresAt);
            }

            return new BucketEntry(existingEntry.bucket(), expiresAt);
        });
    }

    private void clearExpiredBuckets() {
        Instant now = clock.instant();

        buckets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private Bucket createBucket(RateLimitPolicyProperties policy) {
        Bandwidth bandwidth = Bandwidth.classic(policy.capacity(), Refill.intervally(policy.capacity(),
                policy.refillPeriod()));

        return Bucket.builder()
                .withCustomTimePrecision(new ClockTimeMeter(clock))
                .addLimit(bandwidth)
                .build();
    }

    private record BucketEntry(Bucket bucket, Instant expiresAt) {

    }

    private record ClockTimeMeter(Clock clock) implements TimeMeter {

        @Override
        public long currentTimeNanos() {
            return clock.instant().toEpochMilli() * 1_000_000;
        }

        @Override
        public boolean isWallClockBased() {
            return true;
        }
    }
}
