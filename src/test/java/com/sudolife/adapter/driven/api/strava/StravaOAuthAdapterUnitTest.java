package com.sudolife.adapter.driven.api.strava;

import com.sudolife.application.service.strava.StravaAuthorizationRequest;
import org.junit.jupiter.api.Test;

import static com.sudolife.helper.StravaTestHelper.REDIRECT_URI;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.StravaTestHelper.STATE;
import static org.assertj.core.api.Assertions.assertThat;

class StravaOAuthAdapterUnitTest {

    private final StravaOAuthAdapter adapter = new StravaOAuthAdapter("client-id", REDIRECT_URI);

    @Test
    void build_authorization_url_uses_configured_strava_values_and_request_state() {
        StravaAuthorizationRequest request = new StravaAuthorizationRequest(STATE, null, SCOPE);

        String authorizationUrl = adapter.buildAuthorizationUrl(request);

        assertThat(authorizationUrl).startsWith("https://www.strava.com/oauth/authorize?");
        assertThat(authorizationUrl).contains("client_id=client-id");
        assertThat(authorizationUrl).contains("redirect_uri=https://sudolife.com/api/strava/callback");
        assertThat(authorizationUrl).contains("response_type=code");
        assertThat(authorizationUrl).contains("approval_prompt=auto");
        assertThat(authorizationUrl).contains("scope=read");
        assertThat(authorizationUrl).contains("state=state-token");
    }
}
