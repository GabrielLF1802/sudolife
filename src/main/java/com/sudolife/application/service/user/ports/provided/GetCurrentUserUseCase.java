package com.sudolife.application.service.user.ports.provided;

import com.sudolife.application.service.user.CurrentUserResult;

public interface GetCurrentUserUseCase {
    CurrentUserResult execute(String email);
}
