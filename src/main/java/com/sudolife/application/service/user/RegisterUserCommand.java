package com.sudolife.application.service.user;

public record RegisterUserCommand(String name, String email, String password) {
}
