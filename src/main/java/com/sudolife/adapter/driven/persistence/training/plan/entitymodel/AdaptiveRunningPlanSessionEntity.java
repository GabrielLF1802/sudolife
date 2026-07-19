package com.sudolife.adapter.driven.persistence.training.plan.entitymodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "adaptive_running_plan_sessions")
@Getter
@Setter
public class AdaptiveRunningPlanSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private AdaptiveRunningPlanEntity plan;
    @Column(name = "original_planned_session_id")
    private Long originalPlannedSessionId;
    @Column(name = "week_number", nullable = false)
    private int weekNumber;
    @Column(name = "session_number", nullable = false)
    private int sessionNumber;
    @Column(name = "session_type", nullable = false)
    private String sessionType;
    @Column(name = "distance_kilometers", nullable = false)
    private double distanceKilometers;
    @Column(name = "target_type", nullable = false)
    private String targetType;
    @Column(name = "minimum_heart_rate")
    private Integer minimumHeartRate;
    @Column(name = "maximum_heart_rate")
    private Integer maximumHeartRate;
    @Column(name = "minimum_perceived_effort")
    private Integer minimumPerceivedEffort;
    @Column(name = "maximum_perceived_effort")
    private Integer maximumPerceivedEffort;
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;
    @Column(nullable = false)
    private String status;
}
