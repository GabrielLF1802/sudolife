package com.sudolife.adapter.driven.persistence.user;

import com.sudolife.application.model.user.PasswordRecoveryToken;
import org.springframework.stereotype.Component;

@Component
public class PasswordRecoveryTokenPersistenceMapper {

    public PasswordRecoveryTokenEntity toEntity(PasswordRecoveryToken domain) {
        PasswordRecoveryTokenEntity entity = new PasswordRecoveryTokenEntity();
        entity.setId(domain.getId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setTokenHash(domain.getTokenHash());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsedAt(domain.getUsedAt());
        entity.setCreatedAt(domain.getCreatedAt());

        return entity;
    }

    public PasswordRecoveryToken toDomain(PasswordRecoveryTokenEntity entity) {
        return new PasswordRecoveryToken(
                entity.getId(),
                entity.getUserEmail(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt()
        );
    }
}
