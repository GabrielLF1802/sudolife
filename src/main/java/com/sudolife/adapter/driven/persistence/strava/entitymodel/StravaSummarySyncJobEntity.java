package com.sudolife.adapter.driven.persistence.strava.entitymodel;

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
        name = "strava_summary_sync_jobs",
        indexes = {
                @Index(name = "ix_strava_summary_sync_jobs_runnable", columnList = "status, run_after, created_at"),
                @Index(name = "ix_strava_summary_sync_jobs_account_link", columnList = "account_link_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_strava_summary_sync_jobs_open_account_link",
                        columnNames = "open_account_link_id"
                )
        }
)
@Getter
@Setter
public class StravaSummarySyncJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_link_id", nullable = false)
    private Long accountLinkId;
    @Column(name = "open_account_link_id")
    private Long openAccountLinkId;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StravaSummarySyncJobStatus status;
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;
    @Column(name = "imported_activity_count", nullable = false)
    private int importedActivityCount;
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
