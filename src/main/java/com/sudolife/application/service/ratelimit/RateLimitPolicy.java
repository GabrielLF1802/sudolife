package com.sudolife.application.service.ratelimit;

public enum RateLimitPolicy {

    LOGIN_IP,
    LOGIN_EMAIL,
    LOGIN_EMAIL_ORIGIN,
    REGISTRATION_ORIGIN,
    REGISTRATION_EMAIL,
    GENERIC_API
}
