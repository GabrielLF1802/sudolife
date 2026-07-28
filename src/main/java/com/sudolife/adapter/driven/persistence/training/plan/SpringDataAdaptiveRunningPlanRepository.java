package com.sudolife.adapter.driven.persistence.training.plan;

import com.sudolife.adapter.driven.persistence.training.plan.entitymodel.AdaptiveRunningPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataAdaptiveRunningPlanRepository extends JpaRepository<AdaptiveRunningPlanEntity, Long> {

    Optional<AdaptiveRunningPlanEntity> findFirstByUserEmailOrderByAcceptedAtDescIdDesc(String userEmail);

    @Query("""
            select plan from AdaptiveRunningPlanEntity plan
            where not exists (
                select newer.id from AdaptiveRunningPlanEntity newer
                where newer.userEmail = plan.userEmail
                and (newer.acceptedAt > plan.acceptedAt
                    or (newer.acceptedAt = plan.acceptedAt and newer.id > plan.id))
            )
            """)
    List<AdaptiveRunningPlanEntity> findLatestPlans();
}
