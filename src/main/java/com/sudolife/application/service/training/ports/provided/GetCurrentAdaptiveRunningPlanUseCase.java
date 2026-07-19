package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.CurrentAdaptiveRunningPlanResult;

public interface GetCurrentAdaptiveRunningPlanUseCase {

    CurrentAdaptiveRunningPlanResult execute(String userEmail);
}
