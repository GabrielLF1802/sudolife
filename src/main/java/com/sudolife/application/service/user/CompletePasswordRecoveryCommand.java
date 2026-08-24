package com.sudolife.application.service.user;

public record CompletePasswordRecoveryCommand(String token, String newPassword) {
}
