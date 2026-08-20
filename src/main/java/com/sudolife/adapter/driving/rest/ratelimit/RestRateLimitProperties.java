package com.sudolife.adapter.driving.rest.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("api.rate-limit")
public record RestRateLimitProperties(RateLimitPolicyProperties loginIp,
                                      RateLimitPolicyProperties loginEmail,
                                      RateLimitPolicyProperties loginEmailOrigin,
                                      RateLimitPolicyProperties registrationOrigin,
                                      RateLimitPolicyProperties registrationEmail,
                                      RateLimitPolicyProperties genericApi) {

    public RestRateLimitProperties {
        loginIp = requirePolicy(loginIp, "login-ip");
        loginEmail = requirePolicy(loginEmail, "login-email");
        loginEmailOrigin = requirePolicy(loginEmailOrigin, "login-email-origin");
        registrationOrigin = requirePolicy(registrationOrigin, "registration-origin");
        registrationEmail = requirePolicy(registrationEmail, "registration-email");
        genericApi = requirePolicy(genericApi, "generic-api");
    }

    public RateLimitPolicyProperties policy(RestRateLimitPolicy policy) {
        return switch (policy) {
            case LOGIN_IP -> loginIp;
            case LOGIN_EMAIL -> loginEmail;
            case LOGIN_EMAIL_ORIGIN -> loginEmailOrigin;
            case REGISTRATION_ORIGIN -> registrationOrigin;
            case REGISTRATION_EMAIL -> registrationEmail;
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
