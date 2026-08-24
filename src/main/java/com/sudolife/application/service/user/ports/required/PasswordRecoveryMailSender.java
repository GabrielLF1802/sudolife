package com.sudolife.application.service.user.ports.required;

import com.sudolife.application.service.user.PasswordRecoveryEmail;

public interface PasswordRecoveryMailSender {

    void send(PasswordRecoveryEmail email);
}
