package com.sudolife.application.service.training;

import java.util.List;

public record RunningGoalAssessmentResult(
        boolean realistic,
        List<RunningGoalAssessmentReason> reasons,
        RunningGoalResult longTermGoal,
        RunningGoalResult safeMilestone
) {
}
