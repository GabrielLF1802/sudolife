package com.sudolife.application.service.strava;

import java.util.List;

public record StravaActivityListResult(List<StravaActivityListItemResult> activities, int page, int size,
                                       long totalElements, int totalPages) {
}
