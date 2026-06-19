package com.sudolife.adapter.driven.api.strava;

import com.sudolife.application.service.strava.StravaAuthorizationRequest;
import com.sudolife.application.service.strava.exception.StravaAuthorizationFailureException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.sudolife.helper.StravaTestHelper.REDIRECT_URI;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static org.assertj.core.api.Assertions.assertThat;

class StravaOAuthAdapterUnitTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private static final String AUTHORIZATION_URL = "https://www.strava.com/oauth/authorize";
    private static final String TOKEN_URL = "https://www.strava.com/api/v3/oauth/token";
    private static final String DEAUTHORIZATION_URL = "https://www.strava.com/oauth/deauthorize";
    private final StravaOAuthAdapter adapter = new StravaOAuthAdapter(stravaApiProperties());

    @Test
    void build_authorization_url_uses_configured_strava_values_and_request_state() {
        StravaAuthorizationRequest request = new StravaAuthorizationRequest(STATE, null, SCOPE);

        String authorizationUrl = adapter.buildAuthorizationUrl(request);

        assertThat(authorizationUrl).startsWith(AUTHORIZATION_URL + "?");
        assertThat(authorizationUrl).contains("client_id=client-id");
        assertThat(authorizationUrl).contains("redirect_uri=https%3A%2F%2Fsudolife.com%2Fapi%2Fstrava%2Fcallback");
        assertThat(authorizationUrl).contains("response_type=code");
        assertThat(authorizationUrl).contains("approval_prompt=force");
        assertThat(authorizationUrl).contains("scope=read");
        assertThat(authorizationUrl).contains("state=state-token");
    }

    @Test
    void failure_exception_message_does_not_include_sensitive_values() {
        StravaAuthorizationFailureException exception = new StravaAuthorizationFailureException(
                new RuntimeException("access-token refresh-token authorization-code client-secret"));

        String message = exception.getMessage();

        assertThat(message).doesNotContain("access-token");
        assertThat(message).doesNotContain("refresh-token");
        assertThat(message).doesNotContain("authorization-code");
        assertThat(message).doesNotContain("client-secret");
        assertThat(exception.getCause()).isNull();
    }

    private StravaApiProperties stravaApiProperties() {
        return new StravaApiProperties(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, AUTHORIZATION_URL, TOKEN_URL,
                DEAUTHORIZATION_URL, Duration.ofSeconds(2), Duration.ofSeconds(5));
    }
}
