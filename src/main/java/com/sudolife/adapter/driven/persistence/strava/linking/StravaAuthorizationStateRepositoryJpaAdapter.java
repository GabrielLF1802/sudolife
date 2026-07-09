package com.sudolife.adapter.driven.persistence.strava.linking;

import com.sudolife.adapter.driven.persistence.strava.linking.SpringDataStravaAuthorizationStateRepository;
import com.sudolife.adapter.driven.persistence.strava.linking.StravaAuthorizationStatePersistenceMapper;
import com.sudolife.application.model.strava.StravaAuthorizationState;
import com.sudolife.application.service.strava.ports.required.StravaAuthorizationStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    @Transactional
    public Optional<StravaAuthorizationState> consumePending(String state, Instant now, Instant consumedAt) {
        int consumedRows = jpaRepository.consumePending(state, now, consumedAt);
        if (consumedRows == 0) {
            return Optional.empty();
        }

        return findByState(state);
    }

    @Override
    public StravaAuthorizationState save(StravaAuthorizationState authorizationState) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(authorizationState)));
    }
}
