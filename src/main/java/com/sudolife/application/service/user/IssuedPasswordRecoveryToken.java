package com.sudolife.application.service.user;

public record IssuedPasswordRecoveryToken(String rawToken, String tokenHash) {
}
