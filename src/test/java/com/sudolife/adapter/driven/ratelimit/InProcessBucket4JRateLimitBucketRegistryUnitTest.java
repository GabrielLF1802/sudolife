package com.sudolife.adapter.driven.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitBucketKey;
import com.sudolife.application.service.ratelimit.RateLimitConsumption;
import com.sudolife.application.service.ratelimit.RateLimitPolicy;
import com.sudolife.application.service.ratelimit.RateLimitPolicyProperties;
import com.sudolife.config.security.infraestructure.ratelimit.RestRateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InProcessBucket4JRateLimitBucketRegistryUnitTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-13T12:00:00Z"));

    @Test
    void consume_with_available_tokens_allows_request() {
        InProcessBucket4JRateLimitBucketRegistry registry = registry(enabledPolicy(2, Duration.ofMinutes(1)));

        RateLimitConsumption consumption = registry.consume(loginIpKey());

        assertThat(consumption).isEqualTo(RateLimitConsumption.allowed(1));
    }

    @Test
    void consume_with_empty_bucket_blocks_request() {
        InProcessBucket4JRateLimitBucketRegistry registry = registry(enabledPolicy(1, Duration.ofMinutes(1)));
        registry.consume(loginIpKey());

        RateLimitConsumption consumption = registry.consume(loginIpKey());

        assertThat(consumption.allowed()).isFalse();
        assertThat(consumption.remainingTokens()).isZero();
        assertThat(consumption.retryAfter()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void consume_after_refill_period_allows_request() {
        InProcessBucket4JRateLimitBucketRegistry registry = registry(enabledPolicy(1, Duration.ofMinutes(1)));
        registry.consume(loginIpKey());
        clock.advance(Duration.ofMinutes(1));

        RateLimitConsumption consumption = registry.consume(loginIpKey());

        assertThat(consumption).isEqualTo(RateLimitConsumption.allowed(0));
    }

    @Test
    void stored_buckets_with_expired_window_are_removed() {
        InProcessBucket4JRateLimitBucketRegistry registry = registry(enabledPolicy(1, Duration.ofMinutes(1)));
        registry.consume(loginIpKey());
        clock.advance(Duration.ofMinutes(1));

        int storedBucketCount = registry.storedBucketCount();

        assertThat(storedBucketCount).isZero();
    }

    @Test
    void clear_removes_bucket_entry() {
        InProcessBucket4JRateLimitBucketRegistry registry = registry(enabledPolicy(1, Duration.ofMinutes(1)));
        registry.consume(loginIpKey());

        registry.clear(loginIpKey());

        assertThat(registry.consume(loginIpKey())).isEqualTo(RateLimitConsumption.allowed(0));
    }

    @Test
    void consume_with_disabled_policy_allows_without_storing_bucket() {
        InProcessBucket4JRateLimitBucketRegistry registry = registry(disabledPolicy());

        RateLimitConsumption consumption = registry.consume(loginIpKey());

        assertThat(consumption).isEqualTo(RateLimitConsumption.allowedWithoutLimit());
        assertThat(registry.storedBucketCount()).isZero();
    }

    private InProcessBucket4JRateLimitBucketRegistry registry(RateLimitPolicyProperties loginIpPolicy) {
        return new InProcessBucket4JRateLimitBucketRegistry(properties(loginIpPolicy), clock);
    }

    private RestRateLimitProperties properties(RateLimitPolicyProperties loginIpPolicy) {
        RateLimitPolicyProperties fallbackPolicy = enabledPolicy(100, Duration.ofMinutes(1));

        return new RestRateLimitProperties(loginIpPolicy, fallbackPolicy, fallbackPolicy, fallbackPolicy,
                fallbackPolicy, fallbackPolicy, fallbackPolicy, fallbackPolicy, fallbackPolicy);
    }

    private RateLimitPolicyProperties enabledPolicy(long capacity, Duration refillPeriod) {
        return new RateLimitPolicyProperties(true, capacity, refillPeriod);
    }

    private RateLimitPolicyProperties disabledPolicy() {
        return new RateLimitPolicyProperties(false, 0, null);
    }

    private RateLimitBucketKey loginIpKey() {
        return new RateLimitBucketKey(RateLimitPolicy.LOGIN_IP, "203.0.113.10");
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
