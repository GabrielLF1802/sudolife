package com.sudolife.adapter.driving.rest.user;

import com.sudolife.adapter.driving.rest.user.webmodel.UserRequest;
import com.sudolife.application.model.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserWebMapper {
    public User toDomain(UserRequest dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        return user;
    }
}