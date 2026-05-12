package com.sudolife.application.service.strava;

import org.junit.jupiter.api.Test;

import static com.sudolife.helper.StravaTestHelper.CODE;
import static com.sudolife.helper.StravaTestHelper.completeStravaAccountLinkingCommand;
import static org.assertj.core.api.Assertions.assertThat;

class CompleteStravaAccountLinkingCommandUnitTest {

    @Test
    void to_string_redacts_authorization_code() {
        CompleteStravaAccountLinkingCommand command = completeStravaAccountLinkingCommand();

        String value = command.toString();

        assertThat(value).doesNotContain(CODE);
        assertThat(value).contains("code=<redacted>");
    }
}
