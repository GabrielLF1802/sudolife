package com.sudolife.adapter.driven.persistence.training.plan.repository;

import com.sudolife.adapter.driven.persistence.training.plan.AdaptiveRunningPlanPersistenceMapper;
import com.sudolife.adapter.driven.persistence.training.plan.SpringDataAdaptiveRunningPlanRepository;
import com.sudolife.application.model.training.AdaptiveRunningPlan;
import com.sudolife.application.service.training.ports.required.AdaptiveRunningPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdaptiveRunningPlanRepositoryJpaAdapter implements AdaptiveRunningPlanRepository {

    private final SpringDataAdaptiveRunningPlanRepository jpaRepository;
    private final AdaptiveRunningPlanPersistenceMapper mapper;

    @Override
    public AdaptiveRunningPlan save(AdaptiveRunningPlan plan) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(plan)));
    }

    @Override
    public Optional<AdaptiveRunningPlan> findLatestByUserEmail(String userEmail) {
        return jpaRepository.findFirstByUserEmailOrderByAcceptedAtDescIdDesc(userEmail).map(mapper::toDomain);
    }
}
