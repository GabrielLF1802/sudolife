package com.sudolife.adapter.driven.persistence.strava.activity;

import com.sudolife.adapter.driven.persistence.strava.activity.entitymodel.StravaActivityStreamSnapshotEntity;
import com.sudolife.application.model.strava.StravaActivityStreamSnapshot;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StravaActivityStreamSnapshotPersistenceMapper {

    public StravaActivityStreamSnapshotEntity toEntity(StravaActivityStreamSnapshot snapshot) {
        StravaActivityStreamSnapshotEntity entity = new StravaActivityStreamSnapshotEntity();
        entity.setId(snapshot.getId());
        entity.setActivitySummaryId(snapshot.getActivitySummaryId());
        entity.setAccountLinkId(snapshot.getAccountLinkId());
        entity.setUserEmail(snapshot.getUserEmail());
        entity.setSourceActivityId(snapshot.getSourceActivityId());
        entity.setAvailableMetricNames(String.join(",", snapshot.getAvailableMetricNames()));
        entity.setStreamSamplesJson(snapshot.getStreamSamplesJson());
        entity.setFetchedAt(snapshot.getFetchedAt());

        return entity;
    }

    public StravaActivityStreamSnapshot toDomain(StravaActivityStreamSnapshotEntity entity) {
        return new StravaActivityStreamSnapshot(entity.getId(), entity.getActivitySummaryId(),
                entity.getAccountLinkId(), entity.getUserEmail(), entity.getSourceActivityId(),
                metricNames(entity.getAvailableMetricNames()),
                entity.getStreamSamplesJson(), entity.getFetchedAt());
    }

    private List<String> metricNames(String value) {
        return Arrays.stream(value.split(","))
                .filter(metric -> !metric.isBlank())
                .toList();
    }
}
