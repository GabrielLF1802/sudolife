package com.sudolife.adapter.driven.api.strava.authorization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StravaAthleteResponse(@JsonProperty("id") Long id) {
}
