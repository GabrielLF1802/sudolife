package com.sudolife.application.model.training;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TrainingProfile {

    private Long id;
    private String userEmail;
    private Integer birthYear;
    private List<TrainingHeartRateZone> importedHeartRateZones;

    public TrainingProfile(Long id, String userEmail, Integer birthYear) {
        this(id, userEmail, birthYear, List.of());
    }

    public TrainingProfile(Long id, String userEmail, Integer birthYear, List<TrainingHeartRateZone> importedHeartRateZones) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User email is invalid, null or empty");
        }

        this.id = id;
        this.userEmail = userEmail;
        this.birthYear = birthYear;
        this.importedHeartRateZones = List.copyOf(importedHeartRateZones == null ? List.of() : importedHeartRateZones);
    }

    public boolean hasImportedHeartRateZones() {
        return !importedHeartRateZones.isEmpty();
    }
}
