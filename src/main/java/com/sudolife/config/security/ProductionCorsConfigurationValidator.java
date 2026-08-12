package com.sudolife.config.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
@Profile("prod")
class ProductionCorsConfigurationValidator implements InitializingBean {

    private final CorsProperties corsProperties;

    ProductionCorsConfigurationValidator(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> allowedOrigins = corsProperties.allowedOrigins();
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw invalidCorsConfiguration();
        }

        allowedOrigins.forEach(this::validateOrigin);
    }

    private void validateOrigin(String origin) {
        if (origin == null || origin.isBlank() || "*".equals(origin)) {
            throw invalidCorsConfiguration();
        }

        URI uri = parse(origin);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw invalidCorsConfiguration();
        }

        if (uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw invalidCorsConfiguration();
        }

        if (uri.getRawPath() != null && !uri.getRawPath().isBlank()) {
            throw invalidCorsConfiguration();
        }
    }

    private URI parse(String origin) {
        try {
            return URI.create(origin);
        } catch (IllegalArgumentException exception) {
            throw invalidCorsConfiguration();
        }
    }

    private IllegalStateException invalidCorsConfiguration() {
        return new IllegalStateException("Production CORS allowed origins must be explicit HTTPS origins");
    }
}
