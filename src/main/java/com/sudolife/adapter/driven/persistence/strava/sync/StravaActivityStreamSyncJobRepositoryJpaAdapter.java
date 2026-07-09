package com.sudolife.adapter.driven.persistence.strava.sync;

import com.sudolife.adapter.driven.persistence.strava.sync.SpringDataStravaActivityStreamSyncJobRepository;
import com.sudolife.adapter.driven.persistence.strava.sync.StravaActivityStreamSyncJobPersistenceMapper;
import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import com.sudolife.application.service.strava.ports.required.StravaActivityStreamSyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StravaActivityStreamSyncJobRepositoryJpaAdapter implements StravaActivityStreamSyncJobRepository {

    private final SpringDataStravaActivityStreamSyncJobRepository jpaRepository;
    private final StravaActivityStreamSyncJobPersistenceMapper mapper;

    @Override
    public boolean enqueueIfAbsent(StravaActivityStreamSyncJob job) {
        if (jpaRepository.existsByOpenActivitySummaryId(job.getActivitySummaryId())) {
            return false;
        }

        try {
            jpaRepository.saveAndFlush(mapper.toEntity(job));

            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    @Override
    public Optional<StravaActivityStreamSyncJob> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<StravaActivityStreamSyncJob> findNextRunnable(Instant now) {
        return jpaRepository.findByStatusAndRunAfterLessThanEqualOrderByPriorityAscRunAfterAscCreatedAtAsc(
                        StravaSummarySyncJobStatus.QUEUED, now, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public StravaActivityStreamSyncJob save(StravaActivityStreamSyncJob job) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(job)));
    }
}
