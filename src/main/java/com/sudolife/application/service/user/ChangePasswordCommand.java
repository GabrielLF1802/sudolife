package com.sudolife.application.service.user;

public record ChangePasswordCommand(String email, String currentPassword, String newPassword) {
}
