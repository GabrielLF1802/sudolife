package com.sudolife.application.service.training.ports.required;

import com.sudolife.application.service.training.AiRunningPlanProposal;
import com.sudolife.application.service.training.TrainingSnapshot;

public interface AiRunningPlanProvider {

    AiRunningPlanProposal draft(TrainingSnapshot snapshot);
}
