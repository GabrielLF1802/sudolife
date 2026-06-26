package com.sudolife.adapter.driving.rest.strava.webmodel;

import java.util.List;

public record StravaActivityListResponse(List<StravaActivityListItemResponse> activities, int page, int size,
                                         long totalElements, int totalPages) {
}
