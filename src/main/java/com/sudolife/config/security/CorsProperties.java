package com.sudolife.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("api.cors")
public record CorsProperties(List<String> allowedOrigins,
                             List<String> allowedMethods,
                             List<String> allowedHeaders,
                             boolean allowCredentials,
                             Duration maxAge) {

}
