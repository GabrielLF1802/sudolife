package com.sudolife.application.service.strava;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StravaAuthorizationStateGeneratorUnitTest {

    private final StravaAuthorizationStateGenerator generator = new StravaAuthorizationStateGenerator();

    @Test
    void generate_returns_url_safe_single_use_state_values() {
        String firstState = generator.generate();

        String secondState = generator.generate();

        assertThat(firstState).matches("[A-Za-z0-9_-]+");
        assertThat(firstState).hasSize(43);
        assertThat(secondState).isNotEqualTo(firstState);
    }
}
