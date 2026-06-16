package com.sudolife.adapter.driven.api.strava.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StravaAthleteResponse(@JsonProperty("id") Long id) {
}
