package com.sudolife.application.service.training.ports.required;

import com.sudolife.application.model.training.TrainingProfile;

import java.util.Optional;

public interface TrainingProfileRepository {

    Optional<TrainingProfile> findByUserEmail(String userEmail);

    TrainingProfile save(TrainingProfile profile);
}
