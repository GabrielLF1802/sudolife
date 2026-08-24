package com.sudolife.adapter.driven.mail;

import com.sudolife.application.service.user.PasswordRecoveryEmail;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@Profile("!prod")
public class LoggingPasswordRecoveryMailSender implements PasswordRecoveryMailSender {

    private final String frontendBaseUrl;

    public LoggingPasswordRecoveryMailSender(
            @Value("${password-recovery.mail.frontend-base-url}") String frontendBaseUrl
    ) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void send(PasswordRecoveryEmail email) {
        log.info("Local password recovery link for {}: {}", email.email(), recoveryLink(email.token()));
    }

    private String recoveryLink(String token) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/password-recovery/complete")
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}
