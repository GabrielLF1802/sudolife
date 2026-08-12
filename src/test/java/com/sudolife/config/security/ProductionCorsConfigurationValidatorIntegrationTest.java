package com.sudolife.config.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionCorsConfigurationValidatorIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProductionCorsConfiguration.class)
            .withPropertyValues(
                    "spring.profiles.active=prod",
                    "api.cors.allowed-methods=GET,POST,OPTIONS",
                    "api.cors.allowed-headers=Authorization,Content-Type",
                    "api.cors.allow-credentials=true",
                    "api.cors.max-age=PT1H"
            );

    @Test
    void application_context_with_explicit_https_origins_starts() {
        contextRunner
                .withPropertyValues("api.cors.allowed-origins=https://app.sudolife.example,https://admin.sudolife.example")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "api.cors.allowed-origins=",
            "api.cors.allowed-origins= ",
            "api.cors.allowed-origins=*",
            "api.cors.allowed-origins=https://app.sudolife.example,*",
            "api.cors.allowed-origins=http://app.sudolife.example",
            "api.cors.allowed-origins=https://app.sudolife.example/login",
            "api.cors.allowed-origins=not-an-origin"
    })
    void application_context_with_invalid_production_origin_fails_startup(String allowedOriginsProperty) {
        contextRunner
                .withPropertyValues(allowedOriginsProperty)
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseMessage("Production CORS allowed origins must be explicit HTTPS origins"));
    }

    @Test
    void application_context_without_production_origins_fails_startup() {
        contextRunner
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasRootCauseMessage("Production CORS allowed origins must be explicit HTTPS origins"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    @Import(ProductionCorsConfigurationValidator.class)
    static class ProductionCorsConfiguration {

    }
}
