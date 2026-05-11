package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaAccountLink;

import java.util.Optional;

public interface StravaAccountLinkRepository {

    Optional<StravaAccountLink> findActiveByUserEmail(String userEmail);

    Optional<StravaAccountLink> findActiveByAthleteId(Long athleteId);

    StravaAccountLink save(StravaAccountLink link);
}
