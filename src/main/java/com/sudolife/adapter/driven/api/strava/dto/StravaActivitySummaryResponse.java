package com.sudolife.adapter.driven.api.strava.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StravaActivitySummaryResponse(@JsonProperty("id") Long id,
                                            @JsonProperty("name") String name,
                                            @JsonProperty("sport_type") String sportType,
                                            @JsonProperty("start_date") String startDate,
                                            @JsonProperty("distance") Double distance,
                                            @JsonProperty("moving_time") Integer movingTime,
                                            @JsonProperty("average_speed") Double averageSpeed,
                                            @JsonProperty("total_elevation_gain") Double totalElevationGain,
                                            @JsonProperty("max_speed") Double maxSpeed,
                                            @JsonProperty("average_heartrate") Double averageHeartRate,
                                            @JsonProperty("max_heartrate") Double maxHeartRate,
                                            @JsonProperty("average_cadence") Double averageCadence,
                                            @JsonProperty("average_watts") Double averageWatts,
                                            @JsonProperty("calories") Double calories) {
}
