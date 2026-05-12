package com.sudolife.application.model.strava;

import org.junit.jupiter.api.Test;

import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static com.sudolife.helper.StravaTestHelper.USER_EMAIL;
import static com.sudolife.helper.StravaTestHelper.consumedAuthorizationState;
import static com.sudolife.helper.StravaTestHelper.pendingAuthorizationState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StravaAuthorizationStateUnitTest {

    @Test
    void pending_creates_unconsumed_state() {
        StravaAuthorizationState authorizationState = StravaAuthorizationState.pending(STATE, USER_EMAIL, EXPIRES_AT);

        assertThat(authorizationState.isPending()).isTrue();
        assertThat(authorizationState.isConsumed()).isFalse();
        assertThat(authorizationState.getConsumedAt()).isNull();
    }

    @Test
    void is_expired_at_returns_false_before_expiration() {
        StravaAuthorizationState authorizationState = pendingAuthorizationState();

        boolean expired = authorizationState.isExpiredAt(NOW);

        assertThat(expired).isFalse();
    }

    @Test
    void is_expired_at_returns_true_at_expiration() {
        StravaAuthorizationState authorizationState = pendingAuthorizationState();

        boolean expired = authorizationState.isExpiredAt(EXPIRES_AT);

        assertThat(expired).isTrue();
    }

    @Test
    void consume_marks_state_consumed() {
        StravaAuthorizationState authorizationState = pendingAuthorizationState();

        authorizationState.consume(NOW);

        assertThat(authorizationState.isConsumed()).isTrue();
        assertThat(authorizationState.isPending()).isFalse();
        assertThat(authorizationState.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void consume_throws_when_state_is_already_consumed() {
        StravaAuthorizationState authorizationState = consumedAuthorizationState();

        assertThatThrownBy(() -> authorizationState.consume(NOW))
                .isInstanceOf(IllegalStateException.class);
    }
}
