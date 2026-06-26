package com.sudolife.application.service.strava;

import com.sudolife.application.model.strava.StravaActivitySummary;

import java.util.List;

public record StravaActivitySummaryPage(List<StravaActivitySummary> activities, int page, int size,
                                        long totalElements, int totalPages) {
}
