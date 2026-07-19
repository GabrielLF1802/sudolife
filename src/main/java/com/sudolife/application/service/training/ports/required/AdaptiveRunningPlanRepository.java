package com.sudolife.application.service.training.ports.required;

import com.sudolife.application.model.training.AdaptiveRunningPlan;

import java.util.Optional;

public interface AdaptiveRunningPlanRepository {

    AdaptiveRunningPlan save(AdaptiveRunningPlan plan);

    Optional<AdaptiveRunningPlan> findLatestByUserEmail(String userEmail);
}
