package com.sudolife.adapter.driving.rest.ratelimit;

public enum RestRateLimitPolicy {

    LOGIN_IP,
    LOGIN_EMAIL,
    LOGIN_EMAIL_ORIGIN,
    REGISTRATION_ORIGIN,
    REGISTRATION_EMAIL,
    GENERIC_API
}
