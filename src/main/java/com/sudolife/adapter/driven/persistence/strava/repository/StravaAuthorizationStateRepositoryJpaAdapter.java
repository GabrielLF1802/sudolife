package com.sudolife.adapter.driven.persistence.strava.repository;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaAuthorizationStateRepository;
import com.sudolife.adapter.driven.persistence.strava.StravaAuthorizationStatePersistenceMapper;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StravaAuthorizationStateRepositoryJpaAdapter implements StravaAuthorizationStateRepository {

    private final SpringDataStravaAuthorizationStateRepository jpaRepository;
    private final StravaAuthorizationStatePersistenceMapper mapper;

    @Override
    public Optional<StravaAuthorizationState> findByState(String state) {
        return jpaRepository.findById(state).map(mapper::toDomain);
    }

    @Override
    public StravaAuthorizationState save(StravaAuthorizationState authorizationState) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(authorizationState)));
    }
}
