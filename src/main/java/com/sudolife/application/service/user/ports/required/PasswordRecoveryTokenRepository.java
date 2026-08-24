package com.sudolife.application.service.user.ports.required;

import com.sudolife.application.model.user.PasswordRecoveryToken;

import java.time.Instant;
import java.util.Optional;

public interface PasswordRecoveryTokenRepository {

    void save(PasswordRecoveryToken token);

    Optional<PasswordRecoveryToken> findByTokenHash(String tokenHash);

    void invalidateActiveTokens(String userEmail, Instant usedAt);
}
