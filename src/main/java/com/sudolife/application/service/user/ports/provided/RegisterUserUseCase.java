package com.sudolife.application.service.user.ports.provided;

import com.sudolife.application.model.user.User;

public interface RegisterUserUseCase {
    User execute(User user);
}
