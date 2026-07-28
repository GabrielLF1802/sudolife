package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.CorrectPlannedSessionMatchCommand;
import com.sudolife.application.service.training.CurrentAdaptiveRunningPlanResult;

public interface CorrectPlannedSessionMatchUseCase {

    CurrentAdaptiveRunningPlanResult execute(String userEmail, CorrectPlannedSessionMatchCommand command);
}
