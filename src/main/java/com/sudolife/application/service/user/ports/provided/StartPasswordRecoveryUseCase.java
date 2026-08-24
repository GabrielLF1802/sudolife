package com.sudolife.application.service.user.ports.provided;

import com.sudolife.application.service.user.PasswordRecoveryStartResult;
import com.sudolife.application.service.user.StartPasswordRecoveryCommand;

public interface StartPasswordRecoveryUseCase {

    PasswordRecoveryStartResult execute(StartPasswordRecoveryCommand command);
}
