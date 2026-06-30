package com.sudolife.application.model.strava;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class StravaActivityStreamSnapshot {

    private Long id;
    private Long activitySummaryId;
    private Long accountLinkId;
    private String userEmail;
    private Long sourceActivityId;
    private List<String> availableMetricNames;
    private String streamSamplesJson;
    private Instant fetchedAt;

    public StravaActivityStreamSnapshot(Long id, Long activitySummaryId, Long accountLinkId, String userEmail,
                                        Long sourceActivityId, List<String> availableMetricNames,
                                        String streamSamplesJson, Instant fetchedAt) {
        validateNumber(activitySummaryId, "Activity summary id is invalid");
        validateNumber(accountLinkId, "Account link id is invalid");
        validateText(userEmail, "User email is invalid");
        validateNumber(sourceActivityId, "Source activity id is invalid");
        validateMetricNames(availableMetricNames);
        validateText(streamSamplesJson, "Stream samples json is invalid");
        validateInstant(fetchedAt, "Fetched at cant be null");

        this.id = id;
        this.activitySummaryId = activitySummaryId;
        this.accountLinkId = accountLinkId;
        this.userEmail = userEmail;
        this.sourceActivityId = sourceActivityId;
        this.availableMetricNames = List.copyOf(availableMetricNames);
        this.streamSamplesJson = streamSamplesJson;
        this.fetchedAt = fetchedAt;
    }

    public static StravaActivityStreamSnapshot fetched(Long activitySummaryId, Long accountLinkId, String userEmail,
                                                       Long sourceActivityId,
                                                       StravaActivityStreamImport streamImport,
                                                       Instant fetchedAt) {
        return new StravaActivityStreamSnapshot(null, activitySummaryId, accountLinkId, userEmail, sourceActivityId,
                streamImport.availableMetricNames(), streamImport.streamSamplesJson(), fetchedAt);
    }

    private void validateMetricNames(List<String> value) {
        if (value == null || value.isEmpty() || value.stream().anyMatch(metric -> metric == null || metric.isBlank())) {
            throw new IllegalArgumentException("Available metric names are invalid");
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateNumber(Long value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
