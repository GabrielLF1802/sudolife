package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.CurrentAdaptiveRunningPlanResult;
import com.sudolife.application.service.training.SubmitPostSessionPerceivedEffortCommand;

public interface SubmitPostSessionPerceivedEffortUseCase {

    CurrentAdaptiveRunningPlanResult execute(String userEmail, SubmitPostSessionPerceivedEffortCommand command);
}
