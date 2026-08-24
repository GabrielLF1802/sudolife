package com.sudolife.application.service.user;

import com.sudolife.application.model.user.InvalidPasswordException;
import com.sudolife.application.model.user.PasswordRecoveryToken;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.user.exception.InvalidPasswordRecoveryTokenException;
import com.sudolife.application.service.user.exception.NewPasswordMatchesCurrentPasswordException;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenProvider;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenRepository;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static com.sudolife.helper.UserTestHelper.EMAIL;
import static com.sudolife.helper.UserTestHelper.NEW_HASHED_PASSWORD;
import static com.sudolife.helper.UserTestHelper.NEW_PASSWORD;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static com.sudolife.helper.UserTestHelper.HASHED_PASSWORD;
import static com.sudolife.helper.UserTestHelper.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompletePasswordRecoveryUseCaseImplUnitTest {

    private static final Instant NOW = Instant.parse("2026-08-24T10:15:30Z");
    private static final String RAW_TOKEN = "raw-recovery-token";
    private static final String TOKEN_HASH = "hashed-recovery-token";

    @Mock
    private PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Mock
    private PasswordRecoveryTokenProvider passwordRecoveryTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserHashPassword userHashPassword;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private CompletePasswordRecoveryUseCaseImpl useCase;

    @Test
    void execute_updates_password_and_consumes_valid_token() {
        when(timeProvider.now()).thenReturn(NOW);
        when(passwordRecoveryTokenProvider.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(passwordRecoveryTokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeToken()));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(NEW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(userHashPassword.hash(NEW_PASSWORD)).thenReturn(NEW_HASHED_PASSWORD);

        useCase.execute(command());

        User savedUser = capturedSavedUser();
        PasswordRecoveryToken savedToken = capturedSavedToken();
        assertThat(savedUser.getPassword().value()).isEqualTo(NEW_HASHED_PASSWORD);
        assertThat(savedToken.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void execute_rejects_unknown_token_without_changing_password() {
        when(timeProvider.now()).thenReturn(NOW);
        when(passwordRecoveryTokenProvider.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(passwordRecoveryTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(InvalidPasswordRecoveryTokenException.class);
        verifyNoInteractions(userRepository, userHashPassword);
    }

    @Test
    void execute_rejects_used_token_without_changing_password() {
        when(timeProvider.now()).thenReturn(NOW);
        when(passwordRecoveryTokenProvider.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(passwordRecoveryTokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(usedToken()));

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(InvalidPasswordRecoveryTokenException.class);
        verifyNoInteractions(userRepository, userHashPassword);
    }

    @Test
    void execute_rejects_expired_token_without_changing_password() {
        when(timeProvider.now()).thenReturn(NOW);
        when(passwordRecoveryTokenProvider.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(passwordRecoveryTokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(expiredToken()));

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(InvalidPasswordRecoveryTokenException.class);
        verifyNoInteractions(userRepository, userHashPassword);
    }

    @Test
    void execute_rejects_token_without_user_without_changing_password() {
        when(timeProvider.now()).thenReturn(NOW);
        when(passwordRecoveryTokenProvider.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(passwordRecoveryTokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeToken()));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command()))
                .isInstanceOf(InvalidPasswordRecoveryTokenException.class);
        verifyNoInteractions(userHashPassword);
    }

    @Test
    void execute_rejects_weak_password_without_consuming_token() {
        when(timeProvider.now()).thenReturn(NOW);
        when(passwordRecoveryTokenProvider.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(passwordRecoveryTokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeToken()));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> useCase.execute(new CompletePasswordRecoveryCommand(RAW_TOKEN, "weak")))
                .isInstanceOf(InvalidPasswordException.class);
        verifyNoInteractions(userHashPassword);
        verify(passwordRecoveryTokenRepository).findByTokenHash(TOKEN_HASH);
        verifyNoMoreInteractions(passwordRecoveryTokenRepository);
    }

    @Test
    void execute_rejects_new_password_matching_current_password_without_consuming_token() {
        when(timeProvider.now()).thenReturn(NOW);
        when(passwordRecoveryTokenProvider.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(passwordRecoveryTokenRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeToken()));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new CompletePasswordRecoveryCommand(RAW_TOKEN, PASSWORD)))
                .isInstanceOf(NewPasswordMatchesCurrentPasswordException.class);
        verify(passwordRecoveryTokenRepository).findByTokenHash(TOKEN_HASH);
        verifyNoMoreInteractions(passwordRecoveryTokenRepository);
    }

    private CompletePasswordRecoveryCommand command() {
        return new CompletePasswordRecoveryCommand(RAW_TOKEN, NEW_PASSWORD);
    }

    private PasswordRecoveryToken activeToken() {
        return new PasswordRecoveryToken(10L, EMAIL, TOKEN_HASH, NOW.plusSeconds(1800), null, NOW.minusSeconds(60));
    }

    private PasswordRecoveryToken usedToken() {
        return new PasswordRecoveryToken(10L, EMAIL, TOKEN_HASH, NOW.plusSeconds(1800), NOW.minusSeconds(10), NOW.minusSeconds(60));
    }

    private PasswordRecoveryToken expiredToken() {
        return new PasswordRecoveryToken(10L, EMAIL, TOKEN_HASH, NOW.minusSeconds(1), null, NOW.minusSeconds(3600));
    }

    private User capturedSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        return captor.getValue();
    }

    private PasswordRecoveryToken capturedSavedToken() {
        ArgumentCaptor<PasswordRecoveryToken> captor = ArgumentCaptor.forClass(PasswordRecoveryToken.class);
        verify(passwordRecoveryTokenRepository).save(captor.capture());

        return captor.getValue();
    }
}
