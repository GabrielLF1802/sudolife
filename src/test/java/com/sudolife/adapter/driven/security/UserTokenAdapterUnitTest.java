package com.sudolife.adapter.driven.security;

import com.sudolife.application.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.sudolife.helper.UserTestHelper.user;
import static org.assertj.core.api.Assertions.assertThat;

class UserTokenAdapterUnitTest {

    private UserTokenAdapter userTokenAdapter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2030-05-01T12:00:00Z"), ZoneOffset.UTC);
        userTokenAdapter = new UserTokenAdapter(clock);
        ReflectionTestUtils.setField(userTokenAdapter, "secret", "unit-test-secret");
        ReflectionTestUtils.setField(userTokenAdapter, "issuer", "sudolife-api");
        ReflectionTestUtils.setField(userTokenAdapter, "expirationMinutes", 120L);
    }

    @Test
    void subjectFrom_returns_email_from_valid_token() {
        User user = user();

        Optional<String> subject = userTokenAdapter.subjectFrom(userTokenAdapter.generateToken(user));

        assertThat(subject).contains(user.getEmail());
    }

    @Test
    void subjectFrom_returns_empty_from_invalid_token() {
        String token = "invalid-token";

        Optional<String> subject = userTokenAdapter.subjectFrom(token);

        assertThat(subject).isEmpty();
    }
}
