package com.sudolife.application.service.user.exception;

public class NewPasswordMatchesCurrentPasswordException extends RuntimeException {

    public NewPasswordMatchesCurrentPasswordException() {
        super("New password must be different from current password");
    }
}
