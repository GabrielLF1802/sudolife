package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaSummarySyncJobEntity;
import com.sudolife.application.model.strava.StravaSummarySyncJob;
import org.springframework.stereotype.Component;

@Component
public class StravaSummarySyncJobPersistenceMapper {

    public StravaSummarySyncJobEntity toEntity(StravaSummarySyncJob job) {
        StravaSummarySyncJobEntity entity = new StravaSummarySyncJobEntity();
        entity.setId(job.getId());
        entity.setAccountLinkId(job.getAccountLinkId());
        entity.setOpenAccountLinkId(job.isQueuedOrRunning() ? job.getAccountLinkId() : null);
        entity.setUserEmail(job.getUserEmail());
        entity.setStatus(job.getStatus());
        entity.setAttemptCount(job.getAttemptCount());
        entity.setImportedActivityCount(job.getImportedActivityCount());
        entity.setRunAfter(job.getRunAfter());
        entity.setStartedAt(job.getStartedAt());
        entity.setCompletedAt(job.getCompletedAt());
        entity.setFailureReason(job.getFailureReason());
        entity.setCreatedAt(job.getCreatedAt());
        entity.setUpdatedAt(job.getUpdatedAt());

        return entity;
    }

    public StravaSummarySyncJob toDomain(StravaSummarySyncJobEntity entity) {
        return new StravaSummarySyncJob(entity.getId(), entity.getAccountLinkId(), entity.getUserEmail(),
                entity.getStatus(), entity.getAttemptCount(), entity.getImportedActivityCount(), entity.getRunAfter(),
                entity.getStartedAt(), entity.getCompletedAt(), entity.getFailureReason(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
