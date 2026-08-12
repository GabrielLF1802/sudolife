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

    public CorsProperties {
        allowedOrigins = normalize(allowedOrigins);
        allowedMethods = normalize(allowedMethods);
        allowedHeaders = normalize(allowedHeaders);
    }

    private static List<String> normalize(List<String> values) {
        if (values == null) {
            return null;
        }

        return values.stream()
                .map(value -> value == null ? null : value.trim())
                .toList();
    }
}
