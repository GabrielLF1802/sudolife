package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.CoachingProfileResult;

public interface GetCoachingProfileUseCase {

    CoachingProfileResult execute(String userEmail);
}
