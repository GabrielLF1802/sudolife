package com.sudolife.application.service.training.ports.provided;

import com.sudolife.application.service.training.RunningGoalAssessmentResult;

public interface EvaluateRunningGoalUseCase {

    RunningGoalAssessmentResult execute(String userEmail);
}
