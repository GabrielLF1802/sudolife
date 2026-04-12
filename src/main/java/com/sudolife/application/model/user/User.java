package com.sudolife.application.model.user;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String name;
    private String email;
    private String password;

    public User(){}

    public User (Long id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }
}


