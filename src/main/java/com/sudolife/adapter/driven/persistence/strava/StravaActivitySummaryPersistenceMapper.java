package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivitySummaryEntity;
import com.sudolife.application.model.strava.StravaActivitySummary;
import org.springframework.stereotype.Component;

@Component
public class StravaActivitySummaryPersistenceMapper {

    public StravaActivitySummaryEntity toEntity(StravaActivitySummary domain) {
        StravaActivitySummaryEntity entity = new StravaActivitySummaryEntity();
        entity.setId(domain.getId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setAccountLinkId(domain.getAccountLinkId());
        entity.setSourceActivityId(domain.getSourceActivityId());
        entity.setActivityType(domain.getActivityType());
        entity.setRawSportType(domain.getRawSportType());
        entity.setName(domain.getName());
        entity.setStartDate(domain.getStartDate());
        entity.setDistanceMeters(domain.getDistanceMeters());
        entity.setMovingTimeSeconds(domain.getMovingTimeSeconds());
        entity.setAverageSpeedMetersPerSecond(domain.getAverageSpeedMetersPerSecond());
        entity.setPaceSecondsPerKilometer(domain.getPaceSecondsPerKilometer());
        entity.setTotalElevationGainMeters(domain.getTotalElevationGainMeters());
        entity.setMaxSpeedMetersPerSecond(domain.getMaxSpeedMetersPerSecond());
        entity.setAverageHeartRate(domain.getAverageHeartRate());
        entity.setMaxHeartRate(domain.getMaxHeartRate());
        entity.setAverageCadence(domain.getAverageCadence());
        entity.setAverageWatts(domain.getAverageWatts());
        entity.setCalories(domain.getCalories());
        entity.setImportedAt(domain.getImportedAt());

        return entity;
    }

    public StravaActivitySummary toDomain(StravaActivitySummaryEntity entity) {
        return new StravaActivitySummary(entity.getId(), entity.getUserEmail(), entity.getAccountLinkId(),
                entity.getSourceActivityId(), entity.getActivityType(), entity.getRawSportType(), entity.getName(),
                entity.getStartDate(), entity.getDistanceMeters(), entity.getMovingTimeSeconds(),
                entity.getAverageSpeedMetersPerSecond(), entity.getPaceSecondsPerKilometer(),
                entity.getTotalElevationGainMeters(), entity.getMaxSpeedMetersPerSecond(),
                entity.getAverageHeartRate(), entity.getMaxHeartRate(), entity.getAverageCadence(),
                entity.getAverageWatts(), entity.getCalories(), entity.getImportedAt());
    }
}
