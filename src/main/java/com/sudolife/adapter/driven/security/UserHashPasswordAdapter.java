package com.sudolife.adapter.driven.security;

import com.sudolife.application.service.user.ports.required.UserHashPassword;
import org.springframework.stereotype.Component;

@Component
public class UserHashPasswordAdapter implements UserHashPassword {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String password) {
        return passwordEncoder.encode(password);
    }
}
