package com.sudolife.adapter.driving.rest.auth.webmodel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String email;
    private String password;
}