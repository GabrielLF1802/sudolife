package com.sudolife.application.service.strava.ports.required;

import com.sudolife.application.model.training.TrainingHeartRateZone;

import java.util.List;

public interface StravaAthleteProfileProvider {

    List<TrainingHeartRateZone> fetchHeartRateZones(String accessToken);
}
