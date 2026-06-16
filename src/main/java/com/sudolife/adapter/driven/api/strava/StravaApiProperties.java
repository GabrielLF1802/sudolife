package com.sudolife.adapter.driven.api.strava;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("strava")
public record StravaApiProperties(String clientId, String clientSecret, String redirectUri,
                                  String frontendSuccessRedirectUrl, String frontendFailureRedirectUrl,
                                  String authorizationUrl, String tokenUrl, String deauthorizationUrl,
                                  Duration connectTimeout, Duration readTimeout) {
}
