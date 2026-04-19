package com.sudolife.application.service.user.ports.required;

public interface UserHashPassword {
    String hash(String password);
    boolean matches(String rawPassword, String encodedPassword);
}
