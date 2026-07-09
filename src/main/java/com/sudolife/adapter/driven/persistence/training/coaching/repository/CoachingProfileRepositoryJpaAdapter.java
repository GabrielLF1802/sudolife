package com.sudolife.adapter.driven.persistence.training.coaching.repository;

import com.sudolife.adapter.driven.persistence.training.coaching.CoachingProfilePersistenceMapper;
import com.sudolife.adapter.driven.persistence.training.coaching.SpringDataCoachingProfileRepository;
import com.sudolife.adapter.driven.persistence.training.coaching.entitymodel.CoachingProfileEntity;
import com.sudolife.application.model.training.CoachingProfile;
import com.sudolife.application.service.training.ports.required.CoachingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CoachingProfileRepositoryJpaAdapter implements CoachingProfileRepository {

    private final SpringDataCoachingProfileRepository jpaRepository;
    private final CoachingProfilePersistenceMapper mapper;

    @Override
    public Optional<CoachingProfile> findByUserEmail(String userEmail) {
        return jpaRepository.findByUserEmail(userEmail).map(mapper::toDomain);
    }

    @Override
    public CoachingProfile save(CoachingProfile profile) {
        CoachingProfileEntity entity = mapper.toEntity(profile);
        CoachingProfileEntity savedEntity = jpaRepository.saveAndFlush(entity);

        return mapper.toDomain(savedEntity);
    }
}
