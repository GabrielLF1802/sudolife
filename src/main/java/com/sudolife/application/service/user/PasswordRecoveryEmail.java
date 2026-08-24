package com.sudolife.application.service.user;

public record PasswordRecoveryEmail(String email, String token) {
}
