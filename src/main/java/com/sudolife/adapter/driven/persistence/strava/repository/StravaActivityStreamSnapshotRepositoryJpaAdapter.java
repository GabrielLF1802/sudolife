package com.sudolife.adapter.driven.persistence.strava.repository;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaActivityStreamSnapshotRepository;
import com.sudolife.adapter.driven.persistence.strava.StravaActivityStreamSnapshotPersistenceMapper;
import com.sudolife.application.model.strava.StravaActivityStreamSnapshot;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StravaActivityStreamSnapshotRepositoryJpaAdapter implements StravaActivityStreamSnapshotRepository {

    private final SpringDataStravaActivityStreamSnapshotRepository jpaRepository;
    private final StravaActivityStreamSnapshotPersistenceMapper mapper;

    @Override
    public StravaActivityStreamSnapshot saveIfAbsent(StravaActivityStreamSnapshot snapshot) {
        Optional<StravaActivityStreamSnapshot> existingSnapshot = findByActivitySummaryId(snapshot.getActivitySummaryId());
        if (existingSnapshot.isPresent()) {
            return existingSnapshot.get();
        }

        try {
            return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(snapshot)));
        } catch (DataIntegrityViolationException exception) {
            return findByActivitySummaryId(snapshot.getActivitySummaryId()).orElseThrow(() -> exception);
        }
    }

    @Override
    public Optional<StravaActivityStreamSnapshot> findByActivitySummaryId(Long activitySummaryId) {
        return jpaRepository.findByActivitySummaryId(activitySummaryId)
                .map(mapper::toDomain);
    }

    @Override
    public long countByAccountLinkId(Long accountLinkId) {
        return jpaRepository.countByAccountLinkId(accountLinkId);
    }

    @Override
    public Optional<Instant> findLatestFetchedAtByAccountLinkId(Long accountLinkId) {
        return jpaRepository.findByAccountLinkIdOrderByFetchedAtDescIdDesc(accountLinkId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(entity -> entity.getFetchedAt());
    }
}
