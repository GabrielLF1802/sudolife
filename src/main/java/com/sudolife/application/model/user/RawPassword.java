package com.sudolife.application.model.user;

public record RawPassword(String value) {

    public RawPassword {
        if (value == null || value.length() < 6) {
            throw new IllegalArgumentException("Password is invalid!");
        }
    }
}
