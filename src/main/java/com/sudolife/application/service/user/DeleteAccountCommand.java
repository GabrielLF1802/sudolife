package com.sudolife.application.service.user;

public record DeleteAccountCommand(String email, String currentPassword) {

    public DeleteAccountCommand {
        currentPassword = currentPassword == null ? "" : currentPassword;
    }
}
