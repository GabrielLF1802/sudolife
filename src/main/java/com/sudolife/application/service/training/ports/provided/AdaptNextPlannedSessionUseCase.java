package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.AdaptNextPlannedSessionCommand;
import com.sudolife.application.service.training.CurrentAdaptiveRunningPlanResult;

public interface AdaptNextPlannedSessionUseCase {

    CurrentAdaptiveRunningPlanResult execute(String userEmail, AdaptNextPlannedSessionCommand command);
}
