package com.sudolife.application.model.user;

public record HashedPassword(String value) {

    public HashedPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hashed password is invalid!");
        }
    }
}
