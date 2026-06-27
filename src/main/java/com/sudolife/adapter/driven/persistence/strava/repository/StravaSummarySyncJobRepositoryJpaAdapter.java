package com.sudolife.adapter.driven.persistence.strava.repository;

import com.sudolife.adapter.driven.persistence.strava.SpringDataStravaSummarySyncJobRepository;
import com.sudolife.adapter.driven.persistence.strava.StravaSummarySyncJobPersistenceMapper;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import com.sudolife.application.service.strava.ports.required.StravaSummarySyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StravaSummarySyncJobRepositoryJpaAdapter implements StravaSummarySyncJobRepository {

    private static final String OPEN_ACCOUNT_LINK_CONSTRAINT = "uk_strava_summary_sync_jobs_open_account_link";

    private final SpringDataStravaSummarySyncJobRepository jpaRepository;
    private final StravaSummarySyncJobPersistenceMapper mapper;

    @Override
    public boolean enqueueIfAbsent(StravaSummarySyncJob job) {
        if (hasQueuedOrRunningJob(job.getAccountLinkId())) {
            return false;
        }

        try {
            jpaRepository.saveAndFlush(mapper.toEntity(job));

            return true;
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraintViolation(exception, OPEN_ACCOUNT_LINK_CONSTRAINT)) {
                return false;
            }

            throw exception;
        }
    }

    @Override
    public boolean hasQueuedOrRunningJob(Long accountLinkId) {
        return jpaRepository.existsByOpenAccountLinkId(accountLinkId);
    }

    @Override
    public Optional<StravaSummarySyncJob> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<StravaSummarySyncJob> findNextRunnable(Instant now) {
        return jpaRepository.findByStatusAndRunAfterLessThanEqualOrderByRunAfterAscCreatedAtAsc(
                        StravaSummarySyncJobStatus.QUEUED, now, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public StravaSummarySyncJob save(StravaSummarySyncJob job) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toEntity(job)));
    }

    private boolean hasConstraintViolation(Throwable exception, String constraintName) {
        Throwable currentException = exception;
        while (currentException != null) {
            if (hasConstraintName(currentException, constraintName)) {
                return true;
            }

            currentException = currentException.getCause();
        }

        return false;
    }

    private boolean hasConstraintName(Throwable exception, String constraintName) {
        String message = exception.getMessage();

        return message != null && message.toLowerCase().contains(constraintName);
    }
}
