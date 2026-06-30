package com.sudolife.application.model.strava;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@Getter
public class StravaActivityStreamSyncJob {

    private Long id;
    private Long activitySummaryId;
    private Long accountLinkId;
    private String userEmail;
    private Long sourceActivityId;
    private StravaActivityStreamSyncJobPriority priority;
    private StravaSummarySyncJobStatus status;
    private int attemptCount;
    private Instant runAfter;
    private Instant startedAt;
    private Instant completedAt;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public StravaActivityStreamSyncJob(Long id, Long activitySummaryId, Long accountLinkId, String userEmail,
                                       Long sourceActivityId, StravaActivityStreamSyncJobPriority priority,
                                       StravaSummarySyncJobStatus status, int attemptCount, Instant runAfter,
                                       Instant startedAt, Instant completedAt, String failureReason,
                                       Instant createdAt, Instant updatedAt) {
        validate(activitySummaryId, accountLinkId, userEmail, sourceActivityId, priority, status, attemptCount,
                runAfter, createdAt, updatedAt);

        this.id = id;
        this.activitySummaryId = activitySummaryId;
        this.accountLinkId = accountLinkId;
        this.userEmail = userEmail;
        this.sourceActivityId = sourceActivityId;
        this.priority = priority;
        this.status = status;
        this.attemptCount = attemptCount;
        this.runAfter = runAfter;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StravaActivityStreamSyncJob normal(StravaActivitySummary summary, Instant now) {
        return queued(summary, StravaActivityStreamSyncJobPriority.NORMAL, now);
    }

    public static StravaActivityStreamSyncJob highPriority(StravaActivitySummary summary, Instant now) {
        return queued(summary, StravaActivityStreamSyncJobPriority.HIGH, now);
    }

    public StravaActivityStreamSyncJob running(Instant now) {
        return new StravaActivityStreamSyncJob(id, activitySummaryId, accountLinkId, userEmail, sourceActivityId,
                priority, StravaSummarySyncJobStatus.RUNNING, attemptCount + 1, runAfter, now, null, null,
                createdAt, now);
    }

    public StravaActivityStreamSyncJob completed(Instant now) {
        return new StravaActivityStreamSyncJob(id, activitySummaryId, accountLinkId, userEmail, sourceActivityId,
                priority, StravaSummarySyncJobStatus.COMPLETED, attemptCount, runAfter, startedAt, now, null,
                createdAt, now);
    }

    public StravaActivityStreamSyncJob retryableFailure(String failureReason, Instant nextRunAt, Instant now) {
        return new StravaActivityStreamSyncJob(id, activitySummaryId, accountLinkId, userEmail, sourceActivityId,
                priority, StravaSummarySyncJobStatus.QUEUED, attemptCount, nextRunAt, startedAt, null, failureReason,
                createdAt, now);
    }

    public StravaActivityStreamSyncJob permanentFailure(String failureReason, Instant now) {
        return new StravaActivityStreamSyncJob(id, activitySummaryId, accountLinkId, userEmail, sourceActivityId,
                priority, StravaSummarySyncJobStatus.FAILED, attemptCount, runAfter, startedAt, now, failureReason,
                createdAt, now);
    }

    public boolean isQueuedOrRunning() {
        return status == StravaSummarySyncJobStatus.QUEUED || status == StravaSummarySyncJobStatus.RUNNING;
    }

    private static StravaActivityStreamSyncJob queued(StravaActivitySummary summary,
                                                      StravaActivityStreamSyncJobPriority priority, Instant now) {
        return new StravaActivityStreamSyncJob(null, summary.getId(), summary.getAccountLinkId(),
                summary.getUserEmail(), summary.getSourceActivityId(), priority, StravaSummarySyncJobStatus.QUEUED,
                0, now, null, null, null, now, now);
    }

    private void validate(Long activitySummaryId, Long accountLinkId, String userEmail, Long sourceActivityId,
                          StravaActivityStreamSyncJobPriority priority, StravaSummarySyncJobStatus status,
                          int attemptCount, Instant runAfter, Instant createdAt, Instant updatedAt) {
        if (activitySummaryId == null || accountLinkId == null || sourceActivityId == null) {
            throw new IllegalArgumentException("Stream job ids cant be null");
        }

        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User email cant be blank");
        }

        if (priority == null || status == null) {
            throw new IllegalArgumentException("Stream job state cant be null");
        }

        if (attemptCount < 0) {
            throw new IllegalArgumentException("Attempt count cant be negative");
        }

        if (runAfter == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Job timestamps cant be null");
        }
    }
}
