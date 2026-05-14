package com.sudolife.adapter.driven.time;

import com.sudolife.application.service.strava.ports.required.TimeProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SystemTimeProvider implements TimeProvider {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
