package com.sudolife.adapter.driven.persistence.training.repository;

import com.sudolife.adapter.driven.persistence.training.SpringDataTrainingProfileRepository;
import com.sudolife.adapter.driven.persistence.training.TrainingProfilePersistenceMapper;
import com.sudolife.adapter.driven.persistence.training.entitymodel.TrainingProfileEntity;
import com.sudolife.application.model.training.TrainingProfile;
import com.sudolife.application.service.training.ports.required.TrainingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TrainingProfileRepositoryJpaAdapter implements TrainingProfileRepository {

    private final SpringDataTrainingProfileRepository jpaRepository;
    private final TrainingProfilePersistenceMapper mapper;

    @Override
    public Optional<TrainingProfile> findByUserEmail(String userEmail) {
        return jpaRepository.findByUserEmail(userEmail).map(mapper::toDomain);
    }

    @Override
    public TrainingProfile save(TrainingProfile profile) {
        TrainingProfileEntity entity = mapper.toEntity(profile);
        TrainingProfileEntity savedEntity = jpaRepository.saveAndFlush(entity);

        return mapper.toDomain(savedEntity);
    }
}
