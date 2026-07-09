package com.sudolife.adapter.driven.persistence.training;

import com.sudolife.adapter.driven.persistence.training.entitymodel.TrainingProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataTrainingProfileRepository extends JpaRepository<TrainingProfileEntity, Long> {

    Optional<TrainingProfileEntity> findByUserEmail(String userEmail);
}
