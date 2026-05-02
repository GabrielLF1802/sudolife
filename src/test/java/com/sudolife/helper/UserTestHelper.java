package com.sudolife.helper;

import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.AuthenticateUserCommand;
import com.sudolife.application.service.user.RegisterUserCommand;

public class UserTestHelper {

    public static final String EMAIL = "gabriel@sudolife.com";
    public static final String NAME = "Gabriel";
    public static final String PASSWORD = "plain-password";
    public static final String HASHED_PASSWORD = "hashed-password";
    public static final String TOKEN = "jwt-token";

    public static User user() {
        return new User(1L, NAME, EMAIL, HASHED_PASSWORD);
    }

    public static AuthenticateUserCommand authenticateUserCommand() {
        return new AuthenticateUserCommand(EMAIL, PASSWORD);
    }

    public static RegisterUserCommand registerUserCommand() {
        return new RegisterUserCommand(NAME, EMAIL, PASSWORD);
    }
}
