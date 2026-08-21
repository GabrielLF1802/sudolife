package com.sudolife.adapter.driven.persistence.training.plan;

import com.sudolife.adapter.driven.persistence.training.plan.entitymodel.AdaptiveRunningPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query("""
            update AdaptiveRunningPlanSessionEntity session
            set session.originalPlannedSessionId = null
            where session.plan.id in (
                select plan.id
                from AdaptiveRunningPlanEntity plan
                where plan.userEmail = :userEmail
            )
            """)
    void clearOriginalPlannedSessionReferencesByUserEmail(@Param("userEmail") String userEmail);

    @Modifying
    @Query("""
            delete from AdaptiveRunningPlanSessionEntity session
            where session.plan.id in (
                select plan.id
                from AdaptiveRunningPlanEntity plan
                where plan.userEmail = :userEmail
            )
            """)
    void deleteSessionsByUserEmail(@Param("userEmail") String userEmail);

    void deleteByUserEmail(String userEmail);
}
