package com.sudolife.application.service.user.ports.provided;

import com.sudolife.application.service.user.RegisterUserCommand;

public interface RegisterUserUseCase {
    void execute(RegisterUserCommand command);
}
