package com.sudolife.application.service.user;

import com.sudolife.application.model.user.PasswordRecoveryToken;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryMailSender;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenProvider;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenRepository;
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
import static com.sudolife.helper.UserTestHelper.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartPasswordRecoveryUseCaseImplUnitTest {

    private static final Instant NOW = Instant.parse("2026-08-24T10:15:30Z");
    private static final String RAW_TOKEN = "raw-recovery-token";
    private static final String TOKEN_HASH = "hashed-recovery-token";
    private static final String GENERIC_MESSAGE = "Se uma conta existir para esse email, instruções de recuperação de senha serão enviadas.";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;

    @Mock
    private PasswordRecoveryTokenProvider passwordRecoveryTokenProvider;

    @Mock
    private PasswordRecoveryMailSender passwordRecoveryMailSender;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private StartPasswordRecoveryUseCaseImpl useCase;

    @Test
    void execute_creates_recovery_token_and_sends_email_for_registered_email() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(timeProvider.now()).thenReturn(NOW);
        when(passwordRecoveryTokenProvider.provide())
                .thenReturn(new IssuedPasswordRecoveryToken(RAW_TOKEN, TOKEN_HASH));

        PasswordRecoveryStartResult result = useCase.execute(new StartPasswordRecoveryCommand(EMAIL));

        PasswordRecoveryToken token = capturedToken();
        assertThat(result.message()).isEqualTo(GENERIC_MESSAGE);
        assertThat(token.getUserEmail()).isEqualTo(EMAIL);
        assertThat(token.getTokenHash()).isEqualTo(TOKEN_HASH);
        assertThat(token.getCreatedAt()).isEqualTo(NOW);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plusSeconds(1800));
        assertThat(token.getUsedAt()).isNull();
        verify(passwordRecoveryTokenRepository).invalidateActiveTokens(EMAIL, NOW);
        verify(passwordRecoveryMailSender).send(new PasswordRecoveryEmail(EMAIL, RAW_TOKEN));
    }

    @Test
    void execute_returns_same_response_without_creating_token_for_unregistered_email() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        PasswordRecoveryStartResult result = useCase.execute(new StartPasswordRecoveryCommand(EMAIL));

        assertThat(result.message()).isEqualTo(GENERIC_MESSAGE);
        verifyNoInteractions(passwordRecoveryTokenRepository);
        verifyNoInteractions(passwordRecoveryTokenProvider);
        verifyNoInteractions(passwordRecoveryMailSender);
    }

    private PasswordRecoveryToken capturedToken() {
        ArgumentCaptor<PasswordRecoveryToken> captor = ArgumentCaptor.forClass(PasswordRecoveryToken.class);
        verify(passwordRecoveryTokenRepository).save(captor.capture());

        return captor.getValue();
    }
}
