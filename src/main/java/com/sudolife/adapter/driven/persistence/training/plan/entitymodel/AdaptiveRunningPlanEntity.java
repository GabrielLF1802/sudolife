package com.sudolife.adapter.driven.persistence.training.plan.entitymodel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "adaptive_running_plans")
@Getter
@Setter
public class AdaptiveRunningPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Column(name = "safe_milestone_distance_kilometers", nullable = false)
    private double safeMilestoneDistanceKilometers;
    @Column(name = "safe_milestone_pace_seconds_per_kilometer")
    private Integer safeMilestonePaceSecondsPerKilometer;
    @Column(name = "safe_milestone_target_date")
    private LocalDate safeMilestoneTargetDate;
    @Column(nullable = false)
    private String explanation;
    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdaptiveRunningPlanSessionEntity> plannedSessions = new ArrayList<>();
}
