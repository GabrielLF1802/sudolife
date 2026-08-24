package com.sudolife.adapter.driven.mail;

import com.sudolife.application.service.user.PasswordRecoveryEmail;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryMailSender;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class UnavailablePasswordRecoveryMailSender implements PasswordRecoveryMailSender {

    @Override
    public void send(PasswordRecoveryEmail email) {
        throw new IllegalStateException("Password Recovery mail delivery is not configured");
    }
}
