package com.sudolife.application.model.user;

public record Password(String value) {
    public Password {
        if (value == null || value.length() < 6) {
            throw new IllegalArgumentException("Password is invalid!");
        }
    }
}
