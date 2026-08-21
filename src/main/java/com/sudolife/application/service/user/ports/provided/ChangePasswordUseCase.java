package com.sudolife.application.service.user.ports.provided;

import com.sudolife.application.service.user.ChangePasswordCommand;

public interface ChangePasswordUseCase {
    void execute(ChangePasswordCommand command);
}
