package com.sudolife.adapter.driven.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordRecoveryTokenJpaRepository extends JpaRepository<PasswordRecoveryTokenEntity, Long> {

    Optional<PasswordRecoveryTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update PasswordRecoveryTokenEntity token
            set token.usedAt = :usedAt
            where token.userEmail = :userEmail
            and token.usedAt is null
            and token.expiresAt > :usedAt
            """)
    void invalidateActiveTokens(@Param("userEmail") String userEmail, @Param("usedAt") Instant usedAt);

    void deleteByUserEmail(String userEmail);
}
