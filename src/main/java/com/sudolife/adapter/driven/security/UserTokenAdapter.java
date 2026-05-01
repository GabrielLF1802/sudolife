package com.sudolife.adapter.driven.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.ports.required.UserToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserTokenAdapter implements UserToken {

    private final Clock clock;

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.issuer:sudolife-api}")
    private String issuer;

    @Value("${api.security.token.expiration-minutes:120}")
    private long expirationMinutes;

    @Override
    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(user.getEmail())
                    .withExpiresAt(expirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new IllegalStateException("Could not generate user token", exception);
        }
    }

    @Override
    public Optional<String> subjectFrom(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String subject = JWT.require(algorithm)
                    .withIssuer(issuer)
                    .build()
                    .verify(token)
                    .getSubject();

            return Optional.ofNullable(subject);
        } catch (JWTVerificationException exception) {
            return Optional.empty();
        }
    }

    private Instant expirationDate() {
        return Instant.now(clock).plus(expirationMinutes, ChronoUnit.MINUTES);
    }
}
