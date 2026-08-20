package com.sudolife.adapter.driving.rest.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitBucketKey;
import com.sudolife.application.service.ratelimit.RateLimitConsumption;
import com.sudolife.application.service.ratelimit.RateLimitPolicy;
import com.sudolife.application.service.ratelimit.ports.required.RateLimitBucketRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class GenericApiRateLimitFilterUnitTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bucketKey_with_authenticated_user_uses_user_key() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user@sudolife.com",
                null, "USER"));

        RateLimitBucketKey key = filter().bucketKey(request("203.0.113.70"));

        assertThat(key).isEqualTo(new RateLimitBucketKey(RateLimitPolicy.GENERIC_API, "user:user@sudolife.com"));
    }

    @Test
    void bucketKey_with_blank_authenticated_user_falls_back_to_origin_key() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(" ", null, "USER"));

        RateLimitBucketKey key = filter().bucketKey(request("203.0.113.71"));

        assertThat(key).isEqualTo(new RateLimitBucketKey(RateLimitPolicy.GENERIC_API, "origin:203.0.113.71"));
    }

    private GenericApiRateLimitFilter filter() {
        return new GenericApiRateLimitFilter(new AllowedBucketRegistry(), new HttpRequestOriginResolver());
    }

    private MockHttpServletRequest request(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/generic-rate-limit");
        request.addHeader("X-Forwarded-For", origin);

        return request;
    }

    private static class AllowedBucketRegistry implements RateLimitBucketRegistry {

        @Override
        public long availableTokens(RateLimitBucketKey key) {
            return 1;
        }

        @Override
        public RateLimitConsumption consume(RateLimitBucketKey key) {
            return RateLimitConsumption.allowed(0);
        }

        @Override
        public void clear(RateLimitBucketKey key) {
        }
    }
}
