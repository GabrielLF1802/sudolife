package com.sudolife.adapter.driving.rest.strava;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("strava")
public record StravaFrontendRedirectProperties(String frontendSuccessRedirectUrl,
                                               String frontendFailureRedirectUrl) {
}
