package com.sudolife.helper;

import com.sudolife.application.service.user.PasswordRecoveryEmail;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "password-recovery.mail.delivery", havingValue = "stub")
public class StubPasswordRecoveryMailSender implements PasswordRecoveryMailSender {

    @Override
    public void send(PasswordRecoveryEmail email) {
    }
}
