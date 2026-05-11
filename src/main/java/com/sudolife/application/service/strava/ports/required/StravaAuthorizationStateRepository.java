package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaAuthorizationState;

import java.util.Optional;

public interface StravaAuthorizationStateRepository {

    Optional<StravaAuthorizationState> findByState(String state);

    StravaAuthorizationState save(StravaAuthorizationState authorizationState);
}
