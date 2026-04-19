package com.sudolife.application.service.user.ports.provided;

public interface AuthenticateUserUseCase {
    String login(String email, String password);
}
