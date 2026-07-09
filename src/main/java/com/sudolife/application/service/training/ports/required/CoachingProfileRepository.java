package com.sudolife.application.service.training.ports.required;

import com.sudolife.application.model.training.CoachingProfile;

import java.util.Optional;

public interface CoachingProfileRepository {

    Optional<CoachingProfile> findByUserEmail(String userEmail);

    CoachingProfile save(CoachingProfile profile);
}
