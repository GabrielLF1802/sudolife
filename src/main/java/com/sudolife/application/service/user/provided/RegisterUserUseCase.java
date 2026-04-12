package com.sudolife.application.service.user.provided;

import com.sudolife.application.model.user.User;

public interface RegisterUserUseCase {
    User execute(User user);
}
