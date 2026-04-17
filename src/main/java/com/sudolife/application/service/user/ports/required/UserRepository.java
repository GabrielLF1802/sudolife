package com.sudolife.application.service.user.ports.required;

import com.sudolife.application.model.user.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
}

