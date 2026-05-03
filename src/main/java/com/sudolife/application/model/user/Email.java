package com.sudolife.application.model.user;

public record Email(String value) {

    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("The email is invalid!");
        }
    }
}
