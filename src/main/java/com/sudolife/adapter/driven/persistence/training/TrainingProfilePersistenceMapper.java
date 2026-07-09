package com.sudolife.adapter.driven.persistence.training;

import com.sudolife.adapter.driven.persistence.training.entitymodel.TrainingProfileEntity;
import com.sudolife.application.model.training.TrainingHeartRateZone;
import com.sudolife.application.model.training.TrainingProfile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrainingProfilePersistenceMapper {

    public TrainingProfileEntity toEntity(TrainingProfile domain) {
        TrainingProfileEntity entity = new TrainingProfileEntity();
        entity.setId(domain.getId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setBirthYear(domain.getBirthYear());
        List<TrainingHeartRateZone> zones = domain.getImportedHeartRateZones();
        entity.setHeartRateZone1Min(minimum(zones, 0));
        entity.setHeartRateZone1Max(maximum(zones, 0));
        entity.setHeartRateZone2Min(minimum(zones, 1));
        entity.setHeartRateZone2Max(maximum(zones, 1));
        entity.setHeartRateZone3Min(minimum(zones, 2));
        entity.setHeartRateZone3Max(maximum(zones, 2));
        entity.setHeartRateZone4Min(minimum(zones, 3));
        entity.setHeartRateZone4Max(maximum(zones, 3));
        entity.setHeartRateZone5Min(minimum(zones, 4));
        entity.setHeartRateZone5Max(maximum(zones, 4));

        return entity;
    }

    public TrainingProfile toDomain(TrainingProfileEntity entity) {
        return new TrainingProfile(entity.getId(), entity.getUserEmail(), entity.getBirthYear(), zones(entity));
    }

    private List<TrainingHeartRateZone> zones(TrainingProfileEntity entity) {
        return java.util.stream.Stream.of(
                        zone(entity.getHeartRateZone1Min(), entity.getHeartRateZone1Max()),
                        zone(entity.getHeartRateZone2Min(), entity.getHeartRateZone2Max()),
                        zone(entity.getHeartRateZone3Min(), entity.getHeartRateZone3Max()),
                        zone(entity.getHeartRateZone4Min(), entity.getHeartRateZone4Max()),
                        zone(entity.getHeartRateZone5Min(), entity.getHeartRateZone5Max())
                )
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private java.util.Optional<TrainingHeartRateZone> zone(Integer minimumHeartRate, Integer maximumHeartRate) {
        if (minimumHeartRate == null || maximumHeartRate == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new TrainingHeartRateZone(minimumHeartRate, maximumHeartRate));
    }

    private Integer minimum(List<TrainingHeartRateZone> zones, int index) {
        if (zones.size() <= index) {
            return null;
        }

        return zones.get(index).minimumHeartRate();
    }

    private Integer maximum(List<TrainingHeartRateZone> zones, int index) {
        if (zones.size() <= index) {
            return null;
        }

        return zones.get(index).maximumHeartRate();
    }
}
