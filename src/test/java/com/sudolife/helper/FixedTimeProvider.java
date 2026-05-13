package com.sudolife.helper;

import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static com.sudolife.helper.StravaTestHelper.NOW;

@TestConfiguration
public class FixedTimeProvider {

    @Bean
    @Primary
    TimeProvider fixedTimeProvider() {
        return () -> NOW;
    }
}
