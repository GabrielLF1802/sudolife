package com.sudolife.application.model.strava;

import java.util.List;

public record StravaActivityStreamImport(List<String> availableMetricNames, String streamSamplesJson) {
}
