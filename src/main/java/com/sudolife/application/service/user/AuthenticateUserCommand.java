package com.sudolife.application.service.user;

public record AuthenticateUserCommand(String email, String password) {
}
