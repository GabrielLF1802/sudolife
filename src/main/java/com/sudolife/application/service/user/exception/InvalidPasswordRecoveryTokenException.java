package com.sudolife.application.service.user.exception;

public class InvalidPasswordRecoveryTokenException extends IllegalArgumentException {

    public InvalidPasswordRecoveryTokenException() {
        super("Link de recuperação inválido ou expirado. Solicite uma nova recuperação de senha.");
    }
}
