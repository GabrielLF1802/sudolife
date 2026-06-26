package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.strava.StravaAccountLink;

import java.util.List;
import java.util.Optional;

public interface StravaAccountLinkRepository {

    Optional<StravaAccountLink> findActiveById(Long id);

    Optional<StravaAccountLink> findActiveByUserEmail(String userEmail);

    Optional<StravaAccountLink> findActiveByAthleteId(Long athleteId);

    List<StravaAccountLink> findAllActive();

    StravaAccountLink save(StravaAccountLink link);
}
