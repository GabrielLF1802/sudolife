package com.sudolife.application.service.user.ports.required;

import com.sudolife.application.model.user.User;

import java.util.Optional;

public interface UserToken {

    String generateToken(User user);

    Optional<String> subjectFrom(String token);
}
