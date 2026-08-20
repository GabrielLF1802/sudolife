package com.sudolife.adapter.driving.rest.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitBucketKey;
import com.sudolife.application.service.ratelimit.RateLimitConsumption;
import com.sudolife.application.service.ratelimit.RateLimitPolicy;
import com.sudolife.application.service.ratelimit.exception.LoginRateLimitExceededException;
import com.sudolife.application.service.ratelimit.ports.required.RateLimitBucketRegistry;
import com.sudolife.application.service.user.AuthenticateUserCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LoginRateLimitPolicy {

    private static final String MISSING_EMAIL_KEY = "missing-email";

    private final RateLimitBucketRegistry bucketRegistry;

    public void consumePreAuthenticationOrigin(String origin) {
        RateLimitConsumption consumption = bucketRegistry.consume(loginOriginKey(origin));
        if (!consumption.allowed()) {
            throw new LoginRateLimitExceededException();
        }
    }

    public void assertFailedAttemptAllowed(AuthenticateUserCommand command, String origin) {
        if (bucketRegistry.availableTokens(loginEmailKey(command)) < 1) {
            throw new LoginRateLimitExceededException();
        }
        if (bucketRegistry.availableTokens(loginEmailOriginKey(command, origin)) < 1) {
            throw new LoginRateLimitExceededException();
        }
    }

    public void consumeFailedAttempt(AuthenticateUserCommand command, String origin) {
        RateLimitConsumption emailConsumption = bucketRegistry.consume(loginEmailKey(command));
        RateLimitConsumption emailOriginConsumption = bucketRegistry.consume(loginEmailOriginKey(command, origin));
        if (!emailConsumption.allowed() || !emailOriginConsumption.allowed()) {
            throw new LoginRateLimitExceededException();
        }
    }

    public void clearFailedAttempts(AuthenticateUserCommand command, String origin) {
        bucketRegistry.clear(loginEmailKey(command));
        bucketRegistry.clear(loginEmailOriginKey(command, origin));
    }

    private RateLimitBucketKey loginOriginKey(String origin) {
        return new RateLimitBucketKey(RateLimitPolicy.LOGIN_IP, origin);
    }

    private RateLimitBucketKey loginEmailKey(AuthenticateUserCommand command) {
        return new RateLimitBucketKey(RateLimitPolicy.LOGIN_EMAIL, normalizedEmail(command));
    }

    private RateLimitBucketKey loginEmailOriginKey(AuthenticateUserCommand command, String origin) {
        return new RateLimitBucketKey(RateLimitPolicy.LOGIN_EMAIL_ORIGIN, normalizedEmail(command) + "|" + origin);
    }

    private String normalizedEmail(AuthenticateUserCommand command) {
        if (command == null || command.email() == null || command.email().isBlank()) {
            return MISSING_EMAIL_KEY;
        }

        return command.email().trim().toLowerCase(Locale.ROOT);
    }
}
