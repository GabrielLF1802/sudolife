package com.sudolife.adapter.driven.api.strava.athlete.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StravaAthleteZonesResponse(@JsonProperty("heart_rate") StravaHeartRateZoneRangesResponse heartRate) {

    public record StravaHeartRateZoneRangesResponse(@JsonProperty("custom_zones") Boolean customZones,
                                                    List<StravaZoneRangeResponse> zones) {
    }

    public record StravaZoneRangeResponse(Integer min, Integer max) {
    }
}
