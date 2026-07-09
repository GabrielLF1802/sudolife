package com.sudolife.adapter.driven.api.strava.activity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StravaActivityStreamResponse(String type,
                                           @JsonProperty("series_type") String seriesType,
                                           String resolution,
                                           List<Object> data) {
}
