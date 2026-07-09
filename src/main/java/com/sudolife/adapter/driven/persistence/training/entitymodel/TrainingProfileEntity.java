package com.sudolife.adapter.driven.persistence.training.entitymodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "training_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_training_profiles_user_email", columnNames = "user_email")
)
@Getter
@Setter
public class TrainingProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_email", nullable = false)
    private String userEmail;
    @Column(name = "birth_year")
    private Integer birthYear;
    @Column(name = "heart_rate_zone_1_min")
    private Integer heartRateZone1Min;
    @Column(name = "heart_rate_zone_1_max")
    private Integer heartRateZone1Max;
    @Column(name = "heart_rate_zone_2_min")
    private Integer heartRateZone2Min;
    @Column(name = "heart_rate_zone_2_max")
    private Integer heartRateZone2Max;
    @Column(name = "heart_rate_zone_3_min")
    private Integer heartRateZone3Min;
    @Column(name = "heart_rate_zone_3_max")
    private Integer heartRateZone3Max;
    @Column(name = "heart_rate_zone_4_min")
    private Integer heartRateZone4Min;
    @Column(name = "heart_rate_zone_4_max")
    private Integer heartRateZone4Max;
    @Column(name = "heart_rate_zone_5_min")
    private Integer heartRateZone5Min;
    @Column(name = "heart_rate_zone_5_max")
    private Integer heartRateZone5Max;
}
