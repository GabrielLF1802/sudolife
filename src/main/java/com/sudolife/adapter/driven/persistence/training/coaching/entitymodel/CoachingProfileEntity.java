package com.sudolife.adapter.driven.persistence.training.coaching.entitymodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "coaching_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_coaching_profiles_user_email", columnNames = "user_email")
)
@Getter
@Setter
public class CoachingProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Column(name = "target_distance_kilometers", nullable = false)
    private Double targetDistanceKilometers;
    @Column(name = "target_pace_seconds_per_kilometer")
    private Integer targetPaceSecondsPerKilometer;
    @Column(name = "target_date")
    private LocalDate targetDate;
    @Column(name = "readiness", nullable = false)
    private String readiness;
    @Column(name = "injury_concern", nullable = false)
    private boolean injuryConcern;
    @Column(name = "preferred_running_days")
    private String preferredRunningDays;
}
