package com.sudolife.adapter.driven.persistence.strava;

import com.sudolife.adapter.driven.persistence.strava.entitymodel.StravaActivityDetailSnapshotEntity;
import com.sudolife.application.model.strava.StravaActivityDetailSnapshot;
import org.springframework.stereotype.Component;

@Component
public class StravaActivityDetailSnapshotPersistenceMapper {

    public StravaActivityDetailSnapshotEntity toEntity(StravaActivityDetailSnapshot snapshot) {
        StravaActivityDetailSnapshotEntity entity = new StravaActivityDetailSnapshotEntity();
        entity.setId(snapshot.getId());
        entity.setActivitySummaryId(snapshot.getActivitySummaryId());
        entity.setUserEmail(snapshot.getUserEmail());
        entity.setSourceActivityId(snapshot.getSourceActivityId());
        entity.setActivityType(snapshot.getActivityType());
        entity.setRawSportType(snapshot.getRawSportType());
        entity.setName(snapshot.getName());
        entity.setStartDate(snapshot.getStartDate());
        entity.setDistanceMeters(snapshot.getDistanceMeters());
        entity.setMovingTimeSeconds(snapshot.getMovingTimeSeconds());
        entity.setAverageSpeedMetersPerSecond(snapshot.getAverageSpeedMetersPerSecond());
        entity.setPaceSecondsPerKilometer(snapshot.getPaceSecondsPerKilometer());
        entity.setTotalElevationGainMeters(snapshot.getTotalElevationGainMeters());
        entity.setMaxSpeedMetersPerSecond(snapshot.getMaxSpeedMetersPerSecond());
        entity.setAverageHeartRate(snapshot.getAverageHeartRate());
        entity.setMaxHeartRate(snapshot.getMaxHeartRate());
        entity.setAverageCadence(snapshot.getAverageCadence());
        entity.setAverageWatts(snapshot.getAverageWatts());
        entity.setCalories(snapshot.getCalories());
        entity.setFetchedAt(snapshot.getFetchedAt());

        return entity;
    }

    public StravaActivityDetailSnapshot toDomain(StravaActivityDetailSnapshotEntity entity) {
        return new StravaActivityDetailSnapshot(entity.getId(), entity.getActivitySummaryId(), entity.getUserEmail(),
                entity.getSourceActivityId(), entity.getActivityType(), entity.getRawSportType(), entity.getName(),
                entity.getStartDate(), entity.getDistanceMeters(), entity.getMovingTimeSeconds(),
                entity.getAverageSpeedMetersPerSecond(), entity.getPaceSecondsPerKilometer(),
                entity.getTotalElevationGainMeters(), entity.getMaxSpeedMetersPerSecond(),
                entity.getAverageHeartRate(), entity.getMaxHeartRate(), entity.getAverageCadence(),
                entity.getAverageWatts(), entity.getCalories(), entity.getFetchedAt());
    }
}
