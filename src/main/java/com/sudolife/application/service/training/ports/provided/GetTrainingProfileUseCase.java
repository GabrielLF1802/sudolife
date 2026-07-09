package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.TrainingProfileResult;

public interface GetTrainingProfileUseCase {

    TrainingProfileResult execute(String userEmail);
}
