package com.sudolife.application.service.user.ports.required;

import com.sudolife.application.service.user.IssuedPasswordRecoveryToken;

public interface PasswordRecoveryTokenProvider {

    IssuedPasswordRecoveryToken provide();
}
