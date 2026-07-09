package com.sudolife.adapter.driven.persistence.strava.activity.entitymodel;

import com.sudolife.application.model.strava.StravaActivityType;
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
        name = "strava_activity_summaries",
        indexes = {
                @Index(name = "ix_strava_activity_summaries_user_start", columnList = "user_email, start_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_strava_activity_summaries_user_source",
                        columnNames = {"user_email", "source_activity_id"}
                )
        }
)
@Getter
@Setter
public class StravaActivitySummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Column(name = "account_link_id", nullable = false)
    private Long accountLinkId;
    @Column(name = "source_activity_id", nullable = false)
    private Long sourceActivityId;
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private StravaActivityType activityType;
    @Column(name = "raw_sport_type", nullable = false)
    private String rawSportType;
    @Column(nullable = false)
    private String name;
    @Column(name = "start_date", nullable = false)
    private Instant startDate;
    @Column(name = "distance_meters")
    private Double distanceMeters;
    @Column(name = "moving_time_seconds")
    private Integer movingTimeSeconds;
    @Column(name = "average_speed_meters_per_second")
    private Double averageSpeedMetersPerSecond;
    @Column(name = "pace_seconds_per_kilometer")
    private Double paceSecondsPerKilometer;
    @Column(name = "total_elevation_gain_meters")
    private Double totalElevationGainMeters;
    @Column(name = "max_speed_meters_per_second")
    private Double maxSpeedMetersPerSecond;
    @Column(name = "average_heart_rate")
    private Double averageHeartRate;
    @Column(name = "max_heart_rate")
    private Double maxHeartRate;
    @Column(name = "average_cadence")
    private Double averageCadence;
    @Column(name = "average_watts")
    private Double averageWatts;
    @Column(name = "calories")
    private Double calories;
    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;
}
