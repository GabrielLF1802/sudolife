package com.sudolife.application.service.training;

import com.sudolife.application.service.training.exception.AdaptiveRunningPlanNotFoundException;
import com.sudolife.application.service.training.ports.provided.GetCurrentAdaptiveRunningPlanUseCase;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCurrentAdaptiveRunningPlanUseCaseImpl implements GetCurrentAdaptiveRunningPlanUseCase {

    private final AdaptiveRunningPlanRepository adaptiveRunningPlanRepository;

    @Override
    public CurrentAdaptiveRunningPlanResult execute(String userEmail) {
        return adaptiveRunningPlanRepository.findLatestByUserEmail(userEmail)
                .map(CurrentAdaptiveRunningPlanResult::from)
                .orElseThrow(AdaptiveRunningPlanNotFoundException::new);
    }
}
