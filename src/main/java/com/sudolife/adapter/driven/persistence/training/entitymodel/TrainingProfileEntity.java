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
    @Column(name = "birth_year", nullable = false)
    private int birthYear;
}
