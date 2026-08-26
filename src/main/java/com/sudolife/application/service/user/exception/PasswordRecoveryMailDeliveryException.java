package com.sudolife.application.service.user.exception;

public class PasswordRecoveryMailDeliveryException extends RuntimeException {

    public PasswordRecoveryMailDeliveryException() {
        super("Password Recovery mail delivery is temporarily unavailable");
    }

    public PasswordRecoveryMailDeliveryException(Throwable cause) {
        this();
    }
}
