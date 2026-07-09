package com.sudolife.application.service.strava.activity;

import java.util.List;

public record StravaActivityListResult(List<StravaActivityListItemResult> activities, int page, int size,
                                       long totalElements, int totalPages) {
}
