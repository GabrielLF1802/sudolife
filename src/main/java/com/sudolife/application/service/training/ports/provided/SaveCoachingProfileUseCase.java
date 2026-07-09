package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.CoachingProfileResult;
import com.sudolife.application.service.training.SaveCoachingProfileCommand;

public interface SaveCoachingProfileUseCase {

    CoachingProfileResult execute(String userEmail, SaveCoachingProfileCommand command);
}
