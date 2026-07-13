package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.ConservativeRunningPlanResult;

public interface GenerateConservativeRunningPlanUseCase {

    ConservativeRunningPlanResult execute(String userEmail);
}
