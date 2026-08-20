package com.sudolife.config.security.infrastructure.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitPolicyProperties;
import com.sudolife.config.security.infraestructure.ratelimit.RestRateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestRateLimitPropertiesUnitTest {

    @Test
    void policy_with_enabled_zero_capacity_rejects_configuration() {
        assertThatThrownBy(() -> new RateLimitPolicyProperties(true, 0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enabled rate limit capacity must be greater than zero");
    }

    @Test
    void policy_with_enabled_zero_refill_period_rejects_configuration() {
        assertThatThrownBy(() -> new RateLimitPolicyProperties(true, 1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Enabled rate limit refill period must be positive");
    }

    @Test
    void properties_with_missing_policy_rejects_configuration() {
        RateLimitPolicyProperties policy = new RateLimitPolicyProperties(true, 1, Duration.ofMinutes(1));

        assertThatThrownBy(() -> new RestRateLimitProperties(null, policy, policy, policy, policy, policy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rate limit policy login-ip is required");
    }
}
