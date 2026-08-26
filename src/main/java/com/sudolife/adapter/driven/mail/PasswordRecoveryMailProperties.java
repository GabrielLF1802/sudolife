package com.sudolife.adapter.driven.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("password-recovery.mail")
public record PasswordRecoveryMailProperties(
        Resend resend
) {

    public PasswordRecoveryMailProperties {
        resend = resend == null ? new Resend(null, null, "https://api.resend.com", Duration.ofSeconds(2), Duration.ofSeconds(5)) : resend;
    }

    public record Resend(
            String apiKey,
            String sender,
            String apiUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {

        public Resend {
            apiUrl = hasText(apiUrl) ? apiUrl : "https://api.resend.com";
            connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
            readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
        }

        public void validateProductionConfiguration() {
            if (!hasText(apiKey) || !hasText(sender) || !hasText(apiUrl)) {
                throw new IllegalStateException("Production Password Recovery mail delivery requires Resend configuration");
            }
        }

        private static boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
