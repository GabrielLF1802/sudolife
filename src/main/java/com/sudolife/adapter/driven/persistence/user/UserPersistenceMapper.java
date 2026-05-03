package com.sudolife.adapter.driven.persistence.user;

import com.sudolife.application.model.user.User;

import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {
    public UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setPassword(domain.getPassword());
        entity.setEmail(domain.getEmail());

        return entity;
    }

    public User toDomain(UserEntity entity) {
        return new User(
            entity.getId(),
            entity.getName(),
            entity.getEmail(),
            entity.getPassword());
    }
}
