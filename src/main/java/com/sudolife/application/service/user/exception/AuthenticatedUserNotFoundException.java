package com.sudolife.application.service.user.exception;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("Authenticated user not found");
    }
}
