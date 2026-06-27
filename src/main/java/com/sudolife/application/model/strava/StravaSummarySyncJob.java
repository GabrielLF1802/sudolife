package com.sudolife.application.model.strava;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@Getter
public class StravaSummarySyncJob {

    private Long id;
    private Long accountLinkId;
    private String userEmail;
    private StravaSummarySyncJobStatus status;
    private int attemptCount;
    private int importedActivityCount;
    private Instant runAfter;
    private Instant startedAt;
    private Instant completedAt;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public StravaSummarySyncJob(Long id, Long accountLinkId, String userEmail, StravaSummarySyncJobStatus status,
                                int attemptCount, int importedActivityCount, Instant runAfter, Instant startedAt,
                                Instant completedAt, String failureReason, Instant createdAt, Instant updatedAt) {
        validate(accountLinkId, userEmail, status, attemptCount, runAfter, createdAt, updatedAt);

        this.id = id;
        this.accountLinkId = accountLinkId;
        this.userEmail = userEmail;
        this.status = status;
        this.attemptCount = attemptCount;
        this.importedActivityCount = importedActivityCount;
        this.runAfter = runAfter;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StravaSummarySyncJob queued(StravaAccountLink accountLink, Instant now) {
        return new StravaSummarySyncJob(null, accountLink.getId(), accountLink.getUserEmail(),
                StravaSummarySyncJobStatus.QUEUED, 0, 0, now, null, null, null, now, now);
    }

    public StravaSummarySyncJob running(Instant now) {
        return new StravaSummarySyncJob(id, accountLinkId, userEmail, StravaSummarySyncJobStatus.RUNNING,
                attemptCount + 1, importedActivityCount, runAfter, now, null, null, createdAt, now);
    }

    public StravaSummarySyncJob completed(int importedActivityCount, Instant now) {
        return new StravaSummarySyncJob(id, accountLinkId, userEmail, StravaSummarySyncJobStatus.COMPLETED,
                attemptCount, importedActivityCount, runAfter, startedAt, now, null, createdAt, now);
    }

    public StravaSummarySyncJob retryableFailure(String failureReason, int importedActivityCount, Instant nextRunAt,
                                                 Instant now) {
        return new StravaSummarySyncJob(id, accountLinkId, userEmail, StravaSummarySyncJobStatus.QUEUED,
                attemptCount, importedActivityCount, nextRunAt, startedAt, null, failureReason, createdAt, now);
    }

    public StravaSummarySyncJob permanentFailure(String failureReason, int importedActivityCount, Instant now) {
        return new StravaSummarySyncJob(id, accountLinkId, userEmail, StravaSummarySyncJobStatus.FAILED,
                attemptCount, importedActivityCount, runAfter, startedAt, now, failureReason, createdAt, now);
    }

    public boolean isQueuedOrRunning() {
        return status == StravaSummarySyncJobStatus.QUEUED || status == StravaSummarySyncJobStatus.RUNNING;
    }

    private void validate(Long accountLinkId, String userEmail, StravaSummarySyncJobStatus status, int attemptCount,
                          Instant runAfter, Instant createdAt, Instant updatedAt) {
        if (accountLinkId == null) {
            throw new IllegalArgumentException("Account link id cant be null");
        }

        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User email cant be blank");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status cant be null");
        }

        if (attemptCount < 0) {
            throw new IllegalArgumentException("Attempt count cant be negative");
        }

        if (runAfter == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Job timestamps cant be null");
        }
    }
}
