package com.sudolife.adapter.driven.mail;

import com.sudolife.application.service.user.PasswordRecoveryEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PasswordRecoveryLinkBuilder {

    private final String frontendBaseUrl;

    public PasswordRecoveryLinkBuilder(
            @Value("${password-recovery.mail.frontend-base-url}") String frontendBaseUrl
    ) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String build(PasswordRecoveryEmail email) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .pathSegment("password-recovery", "complete")
                .queryParam("token", email.token())
                .build()
                .encode()
                .toUriString();
    }
}
