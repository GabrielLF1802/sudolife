package com.sudolife.adapter.driven.persistence.strava.entitymodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "strava_activity_stream_snapshots",
        indexes = {
                @Index(name = "ix_strava_activity_stream_snapshots_account_link", columnList = "account_link_id"),
                @Index(name = "ix_strava_activity_stream_snapshots_user_source", columnList = "user_email, source_activity_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_strava_activity_stream_snapshots_summary",
                        columnNames = "activity_summary_id"
                )
        }
)
@Getter
@Setter
public class StravaActivityStreamSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "activity_summary_id", nullable = false)
    private Long activitySummaryId;
    @Column(name = "account_link_id", nullable = false)
    private Long accountLinkId;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Column(name = "source_activity_id", nullable = false)
    private Long sourceActivityId;
    @Column(name = "available_metric_names", nullable = false, length = 512)
    private String availableMetricNames;
    @Lob
    @Column(name = "stream_samples_json", nullable = false)
    private String streamSamplesJson;
    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}
