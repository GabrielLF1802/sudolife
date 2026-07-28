package com.sudolife.application.service.training;

import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.service.training.exception.AdaptiveRunningPlanNotFoundException;
import com.sudolife.application.service.training.ports.provided.SubmitPostSessionPerceivedEffortUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitPostSessionPerceivedEffortUseCaseImpl implements SubmitPostSessionPerceivedEffortUseCase {

    private final AdaptiveRunningPlanRepository planRepository;

    @Override
    @Transactional
    public CurrentAdaptiveRunningPlanResult execute(
            String userEmail,
            SubmitPostSessionPerceivedEffortCommand command
    ) {
        AdaptiveRunningPlan plan = planRepository.findLatestByUserEmail(userEmail)
                .orElseThrow(AdaptiveRunningPlanNotFoundException::new);
        plan.findPlannedSession(command.plannedSessionId())
                .reportPostSessionPerceivedEffort(command.perceivedEffort());

        return CurrentAdaptiveRunningPlanResult.from(planRepository.save(plan));
    }
}
