package com.sudolife.application.service.strava.authorization;

import org.junit.jupiter.api.Test;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.stravaTokenAuthorization;
import static org.assertj.core.api.Assertions.assertThat;

class StravaTokenAuthorizationUnitTest {

    @Test
    void to_string_redacts_token_values() {
        StravaTokenAuthorization authorization = stravaTokenAuthorization();

        String value = authorization.toString();

        assertThat(value).doesNotContain(ACCESS_TOKEN);
        assertThat(value).doesNotContain(REFRESH_TOKEN);
        assertThat(value).contains("<redacted>");
    }
}
