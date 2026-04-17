package com.sudolife.adapter.driving.rest.user.webmodel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
    private String email;
    private String name;
    private String password;
}
