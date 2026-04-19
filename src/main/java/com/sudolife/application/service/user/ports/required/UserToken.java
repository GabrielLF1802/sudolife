package com.sudolife.application.service.user.ports.required;

import com.sudolife.application.model.user.User;

public interface UserToken {
    String generateToken (User user);
}
