package com.sudolife.application.service.strava.ports.required;

import java.time.Instant;

public interface TimeProvider {

    Instant now();
}
