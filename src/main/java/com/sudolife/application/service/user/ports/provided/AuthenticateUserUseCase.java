package com.sudolife.application.service.user.ports.provided;

import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.AuthenticationResult;

public interface AuthenticateUserUseCase {
    AuthenticationResult execute(AuthenticateUserCommand command);
}
