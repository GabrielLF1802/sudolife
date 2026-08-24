package com.sudolife.application.service.user.ports.provided;

import com.sudolife.application.service.user.CompletePasswordRecoveryCommand;

public interface CompletePasswordRecoveryUseCase {

    void execute(CompletePasswordRecoveryCommand command);
}
