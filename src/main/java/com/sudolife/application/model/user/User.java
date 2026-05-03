package com.sudolife.application.model.user;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class User {

    private Long id;
    private String name;
    private Email email;
    private Password password;

    public User (Long id, String name, Email email, Password password) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is invalid, null or empty");
        }

        if (email == null) {
            throw new IllegalArgumentException("Email cant be null");
        }

        if (password == null) {
            throw new IllegalArgumentException("password cant be null");
        }

        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
