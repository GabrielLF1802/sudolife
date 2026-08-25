package com.sudolife.adapter.driving.rest.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitBucketKey;
import com.sudolife.application.service.ratelimit.RateLimitConsumption;
import com.sudolife.application.service.ratelimit.RateLimitPolicy;
import com.sudolife.application.service.ratelimit.exception.PasswordRecoveryRateLimitExceededException;
import com.sudolife.application.service.ratelimit.ports.required.RateLimitBucketRegistry;
import com.sudolife.application.service.user.StartPasswordRecoveryCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PasswordRecoveryRateLimitPolicy {

    private static final String MISSING_EMAIL_KEY = "missing-email";

    private final RateLimitBucketRegistry bucketRegistry;

    public void consumeStartAttempt(StartPasswordRecoveryCommand command, String origin) {
        RateLimitConsumption originConsumption = bucketRegistry.consume(startOriginKey(origin));
        if (!originConsumption.allowed()) {
            throw new PasswordRecoveryRateLimitExceededException();
        }

        RateLimitConsumption emailConsumption = bucketRegistry.consume(startEmailKey(command));
        if (!emailConsumption.allowed()) {
            throw new PasswordRecoveryRateLimitExceededException();
        }
    }

    public void consumeCompleteAttempt(String origin) {
        RateLimitConsumption originConsumption = bucketRegistry.consume(completeOriginKey(origin));
        if (!originConsumption.allowed()) {
            throw new PasswordRecoveryRateLimitExceededException();
        }
    }

    private RateLimitBucketKey startOriginKey(String origin) {
        return new RateLimitBucketKey(RateLimitPolicy.PASSWORD_RECOVERY_START_ORIGIN, origin);
    }

    private RateLimitBucketKey startEmailKey(StartPasswordRecoveryCommand command) {
        return new RateLimitBucketKey(RateLimitPolicy.PASSWORD_RECOVERY_START_EMAIL, normalizedEmail(command));
    }

    private RateLimitBucketKey completeOriginKey(String origin) {
        return new RateLimitBucketKey(RateLimitPolicy.PASSWORD_RECOVERY_COMPLETE_ORIGIN, origin);
    }

    private String normalizedEmail(StartPasswordRecoveryCommand command) {
        if (command == null || command.email() == null || command.email().isBlank()) {
            return MISSING_EMAIL_KEY;
        }

        return command.email().trim().toLowerCase(Locale.ROOT);
    }
}
