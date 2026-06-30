package com.sudolife.adapter.driven.persistence.strava.repository;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaActivityDetailSnapshotRepository;
import com.sudolife.adapter.driven.persistence.strava.StravaActivityDetailSnapshotPersistenceMapper;
import com.sudolife.application.model.strava.StravaActivityDetailSnapshot;
import com.sudolife.application.service.strava.ports.required.StravaActivityDetailSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StravaActivityDetailSnapshotRepositoryJpaAdapter implements StravaActivityDetailSnapshotRepository {

    private final SpringDataStravaActivityDetailSnapshotRepository jpaRepository;
    private final StravaActivityDetailSnapshotPersistenceMapper mapper;

    @Override
    public Optional<StravaActivityDetailSnapshot> findByActivitySummaryId(Long activitySummaryId) {
        return jpaRepository.findByActivitySummaryId(activitySummaryId)
                .map(mapper::toDomain);
    }

    @Override
    public StravaActivityDetailSnapshot saveIfAbsent(StravaActivityDetailSnapshot snapshot) {
        if (jpaRepository.existsByActivitySummaryId(snapshot.getActivitySummaryId())) {
            return findByActivitySummaryId(snapshot.getActivitySummaryId()).orElseThrow();
        }

        try {
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(snapshot)));
        } catch (DataIntegrityViolationException exception) {
            return findByActivitySummaryId(snapshot.getActivitySummaryId()).orElseThrow();
        }
    }
}
