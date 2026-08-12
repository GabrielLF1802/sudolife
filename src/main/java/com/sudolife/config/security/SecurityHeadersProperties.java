package com.sudolife.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("api.security.headers")
public record SecurityHeadersProperties(boolean hstsEnabled,
                                        Duration hstsMaxAge,
                                        boolean hstsIncludeSubDomains) {

}
