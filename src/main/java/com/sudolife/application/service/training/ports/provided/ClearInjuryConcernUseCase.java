package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.ClearInjuryConcernCommand;
import com.sudolife.application.service.training.CurrentAdaptiveRunningPlanResult;

public interface ClearInjuryConcernUseCase {

    CurrentAdaptiveRunningPlanResult execute(String userEmail, ClearInjuryConcernCommand command);
}
