package com.sudolife.config.security.infraestructure.ratelimit;

import com.sudolife.application.service.ratelimit.RateLimitPolicyProperties;
import com.sudolife.application.service.ratelimit.RateLimitPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("api.rate-limit")
public record RestRateLimitProperties(RateLimitPolicyProperties loginIp,
                                      RateLimitPolicyProperties loginEmail,
                                      RateLimitPolicyProperties loginEmailOrigin,
                                      RateLimitPolicyProperties registrationOrigin,
                                      RateLimitPolicyProperties registrationEmail,
                                      RateLimitPolicyProperties passwordRecoveryStartOrigin,
                                      RateLimitPolicyProperties passwordRecoveryStartEmail,
                                      RateLimitPolicyProperties passwordRecoveryCompleteOrigin,
                                      RateLimitPolicyProperties genericApi) {

    public RestRateLimitProperties {
        loginIp = requirePolicy(loginIp, "login-ip");
        loginEmail = requirePolicy(loginEmail, "login-email");
        loginEmailOrigin = requirePolicy(loginEmailOrigin, "login-email-origin");
        registrationOrigin = requirePolicy(registrationOrigin, "registration-origin");
        registrationEmail = requirePolicy(registrationEmail, "registration-email");
        passwordRecoveryStartOrigin = requirePolicy(passwordRecoveryStartOrigin, "password-recovery-start-origin");
        passwordRecoveryStartEmail = requirePolicy(passwordRecoveryStartEmail, "password-recovery-start-email");
        passwordRecoveryCompleteOrigin = requirePolicy(passwordRecoveryCompleteOrigin, "password-recovery-complete-origin");
        genericApi = requirePolicy(genericApi, "generic-api");
    }

    public RateLimitPolicyProperties policy(RateLimitPolicy policy) {
        return switch (policy) {
            case LOGIN_IP -> loginIp;
            case LOGIN_EMAIL -> loginEmail;
            case LOGIN_EMAIL_ORIGIN -> loginEmailOrigin;
            case REGISTRATION_ORIGIN -> registrationOrigin;
            case REGISTRATION_EMAIL -> registrationEmail;
            case PASSWORD_RECOVERY_START_ORIGIN -> passwordRecoveryStartOrigin;
            case PASSWORD_RECOVERY_START_EMAIL -> passwordRecoveryStartEmail;
            case PASSWORD_RECOVERY_COMPLETE_ORIGIN -> passwordRecoveryCompleteOrigin;
            case GENERIC_API -> genericApi;
        };
    }

    private static RateLimitPolicyProperties requirePolicy(RateLimitPolicyProperties policy, String name) {
        if (policy == null) {
            throw new IllegalArgumentException("Rate limit policy " + name + " is required");
        }

        return policy;
    }
}
