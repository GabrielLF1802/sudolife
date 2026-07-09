package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.SaveTrainingProfileCommand;
import com.sudolife.application.service.training.TrainingProfileResult;

public interface SaveTrainingProfileUseCase {

    TrainingProfileResult execute(String userEmail, SaveTrainingProfileCommand command);
}
