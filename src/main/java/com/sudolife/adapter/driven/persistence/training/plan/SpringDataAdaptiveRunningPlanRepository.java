package com.sudolife.adapter.driven.persistence.training.plan;

import com.sudolife.adapter.driven.persistence.training.plan.entitymodel.AdaptiveRunningPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataAdaptiveRunningPlanRepository extends JpaRepository<AdaptiveRunningPlanEntity, Long> {

    Optional<AdaptiveRunningPlanEntity> findFirstByUserEmailOrderByAcceptedAtDescIdDesc(String userEmail);
}
