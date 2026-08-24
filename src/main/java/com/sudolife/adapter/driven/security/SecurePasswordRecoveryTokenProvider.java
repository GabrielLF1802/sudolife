package com.sudolife.adapter.driven.security;

import com.sudolife.application.service.user.IssuedPasswordRecoveryToken;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class SecurePasswordRecoveryTokenProvider implements PasswordRecoveryTokenProvider {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public IssuedPasswordRecoveryToken provide() {
        String rawToken = rawToken();

        return new IssuedPasswordRecoveryToken(rawToken, hash(rawToken));
    }

    private String rawToken() {
        byte[] token = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(token);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    @Override
    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
