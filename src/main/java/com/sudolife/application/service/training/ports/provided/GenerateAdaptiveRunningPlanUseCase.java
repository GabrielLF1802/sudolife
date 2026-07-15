package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.AdaptiveRunningPlanResult;

public interface GenerateAdaptiveRunningPlanUseCase {

    AdaptiveRunningPlanResult execute(String userEmail);
}
