package com.sudolife.adapter.driven.persistence.training.coaching;

import com.sudolife.adapter.driven.persistence.training.coaching.entitymodel.CoachingProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataCoachingProfileRepository extends JpaRepository<CoachingProfileEntity, Long> {

    Optional<CoachingProfileEntity> findByUserEmail(String userEmail);
}
