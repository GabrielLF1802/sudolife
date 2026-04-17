package com.sudolife.adapter.driven.persistence.user.repository;

import com.sudolife.adapter.driven.persistence.SpringDataUserRepository;
import com.sudolife.adapter.driven.persistence.UserEntity;
import com.sudolife.adapter.driven.persistence.UserPersistenceMapper;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.ports.required.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryJpaAdapter implements UserRepository {

    private final SpringDataUserRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }
}
