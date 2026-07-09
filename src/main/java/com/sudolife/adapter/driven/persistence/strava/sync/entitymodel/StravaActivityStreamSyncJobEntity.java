package com.sudolife.adapter.driven.persistence.strava.sync.entitymodel;

import com.sudolife.application.model.strava.StravaActivityStreamSyncJobPriority;
import com.sudolife.application.model.strava.StravaSummarySyncJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "strava_activity_stream_sync_jobs",
        indexes = {
                @Index(name = "ix_strava_activity_stream_sync_jobs_runnable", columnList = "status, run_after, priority, created_at"),
                @Index(name = "ix_strava_activity_stream_sync_jobs_account_link", columnList = "account_link_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_strava_activity_stream_sync_jobs_open_summary",
                        columnNames = "open_activity_summary_id"
                )
        }
)
@Getter
@Setter
public class StravaActivityStreamSyncJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "activity_summary_id", nullable = false)
    private Long activitySummaryId;
    @Column(name = "open_activity_summary_id")
    private Long openActivitySummaryId;
    @Column(name = "account_link_id", nullable = false)
    private Long accountLinkId;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Column(name = "source_activity_id", nullable = false)
    private Long sourceActivityId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StravaActivityStreamSyncJobPriority priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StravaSummarySyncJobStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "run_after", nullable = false)
    private Instant runAfter;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "failure_reason", length = 64)
    private String failureReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
