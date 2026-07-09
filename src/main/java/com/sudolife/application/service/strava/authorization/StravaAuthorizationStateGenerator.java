package com.sudolife.application.service.strava.authorization;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class StravaAuthorizationStateGenerator {

    private static final int STATE_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] state = new byte[STATE_BYTES];
        secureRandom.nextBytes(state);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(state);
    }
}
