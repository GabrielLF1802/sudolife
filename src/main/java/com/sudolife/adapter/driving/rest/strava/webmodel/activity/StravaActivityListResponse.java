package com.sudolife.adapter.driving.rest.strava.webmodel.activity;

import java.util.List;

public record StravaActivityListResponse(List<StravaActivityListItemResponse> activities, int page, int size,
                                         long totalElements, int totalPages) {
}
