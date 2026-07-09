package com.sudolife.adapter.driven.persistence.strava.sync;

import com.sudolife.adapter.driven.persistence.strava.sync.entitymodel.StravaActivityStreamSyncJobEntity;
import com.sudolife.application.model.strava.StravaActivityStreamSyncJob;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import org.springframework.stereotype.Component;

@Component
public class StravaActivityStreamSyncJobPersistenceMapper {

    public StravaActivityStreamSyncJobEntity toEntity(StravaActivityStreamSyncJob domain) {
        StravaActivityStreamSyncJobEntity entity = new StravaActivityStreamSyncJobEntity();
        entity.setId(domain.getId());
        entity.setActivitySummaryId(domain.getActivitySummaryId());
        entity.setOpenActivitySummaryId(openActivitySummaryId(domain));
        entity.setAccountLinkId(domain.getAccountLinkId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setSourceActivityId(domain.getSourceActivityId());
        entity.setPriority(domain.getPriority());
        entity.setStatus(domain.getStatus());
        entity.setAttemptCount(domain.getAttemptCount());
        entity.setRunAfter(domain.getRunAfter());
        entity.setStartedAt(domain.getStartedAt());
        entity.setCompletedAt(domain.getCompletedAt());
        entity.setFailureReason(domain.getFailureReason());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }

    public StravaActivityStreamSyncJob toDomain(StravaActivityStreamSyncJobEntity entity) {
        return new StravaActivityStreamSyncJob(entity.getId(), entity.getActivitySummaryId(),
                entity.getAccountLinkId(), entity.getUserEmail(), entity.getSourceActivityId(),
                entity.getPriority(), entity.getStatus(), entity.getAttemptCount(), entity.getRunAfter(),
                entity.getStartedAt(), entity.getCompletedAt(), entity.getFailureReason(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private Long openActivitySummaryId(StravaActivityStreamSyncJob domain) {
        if (domain.getStatus() == StravaSummarySyncJobStatus.QUEUED ||
                domain.getStatus() == StravaSummarySyncJobStatus.RUNNING) {
            return domain.getActivitySummaryId();
        }

        return null;
    }
}
