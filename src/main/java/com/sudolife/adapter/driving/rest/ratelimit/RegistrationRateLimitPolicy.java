package com.sudolife.adapter.driving.rest.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitBucketKey;
import com.sudolife.application.service.ratelimit.RateLimitConsumption;
import com.sudolife.application.service.ratelimit.RateLimitPolicy;
import com.sudolife.application.service.ratelimit.exception.RegisterRateLimitExceededException;
import com.sudolife.application.service.ratelimit.ports.required.RateLimitBucketRegistry;
import com.sudolife.application.service.user.RegisterUserCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RegistrationRateLimitPolicy {

    private static final String MISSING_EMAIL_KEY = "missing-email";

    private final RateLimitBucketRegistry bucketRegistry;

    public void consumeRegistrationAttempt(RegisterUserCommand command, String origin) {
        RateLimitConsumption originConsumption = bucketRegistry.consume(registrationOriginKey(origin));
        if (!originConsumption.allowed()) {
            throw new RegisterRateLimitExceededException();
        }

        RateLimitConsumption emailConsumption = bucketRegistry.consume(registrationEmailKey(command));
        if (!emailConsumption.allowed()) {
            throw new RegisterRateLimitExceededException();
        }
    }

    private RateLimitBucketKey registrationOriginKey(String origin) {
        return new RateLimitBucketKey(RateLimitPolicy.REGISTRATION_ORIGIN, origin);
    }

    private RateLimitBucketKey registrationEmailKey(RegisterUserCommand command) {
        return new RateLimitBucketKey(RateLimitPolicy.REGISTRATION_EMAIL, normalizedEmail(command));
    }

    private String normalizedEmail(RegisterUserCommand command) {
        if (command == null || command.email() == null || command.email().isBlank()) {
            return MISSING_EMAIL_KEY;
        }

        return command.email().trim().toLowerCase(Locale.ROOT);
    }
}
