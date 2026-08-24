package com.sudolife.adapter.driven.persistence.user;

import com.sudolife.application.model.user.PasswordRecoveryToken;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordRecoveryTokenRepositoryJpaAdapter implements PasswordRecoveryTokenRepository {

    private final PasswordRecoveryTokenJpaRepository jpaRepository;
    private final PasswordRecoveryTokenPersistenceMapper mapper;

    @Override
    public void save(PasswordRecoveryToken token) {
        jpaRepository.save(mapper.toEntity(token));
    }

    @Override
    public Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void invalidateActiveTokens(String userEmail, Instant usedAt) {
        jpaRepository.invalidateActiveTokens(userEmail, usedAt);
    }
}
