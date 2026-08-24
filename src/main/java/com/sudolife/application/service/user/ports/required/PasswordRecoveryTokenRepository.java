package com.sudolife.application.service.user.ports.required;

import com.sudolife.application.model.user.PasswordRecoveryToken;

import java.time.Instant;

public interface PasswordRecoveryTokenRepository {

    void save(PasswordRecoveryToken token);

    void invalidateActiveTokens(String userEmail, Instant usedAt);
}
