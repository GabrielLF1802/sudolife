package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.CurrentAdaptiveRunningPlanResult;

public interface UnlinkPlannedSessionMatchUseCase {

    CurrentAdaptiveRunningPlanResult execute(String userEmail, Long plannedSessionId);
}
