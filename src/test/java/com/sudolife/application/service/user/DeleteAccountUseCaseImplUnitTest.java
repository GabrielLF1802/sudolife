package com.sudolife.application.service.user;

import com.sudolife.application.service.strava.authorization.StravaTokenAuthorization;
import com.sudolife.application.service.strava.ports.required.StravaOAuthProvider;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.user.exception.AuthenticatedUserNotFoundException;
import com.sudolife.application.service.user.exception.InvalidCredentialsException;
import com.sudolife.application.service.user.ports.required.AccountDeletionDataRepository;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.sudolife.helper.StravaTestHelper.ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ATHLETE_ID;
import static com.sudolife.helper.StravaTestHelper.EXPIRES_AT;
import static com.sudolife.helper.StravaTestHelper.NOW;
import static com.sudolife.helper.StravaTestHelper.REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ROTATED_ACCESS_TOKEN;
import static com.sudolife.helper.StravaTestHelper.ROTATED_REFRESH_TOKEN;
import static com.sudolife.helper.StravaTestHelper.SCOPE;
import static com.sudolife.helper.UserTestHelper.EMAIL;
import static com.sudolife.helper.UserTestHelper.HASHED_PASSWORD;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static com.sudolife.helper.UserTestHelper.deleteAccountCommand;
import static com.sudolife.helper.UserTestHelper.user;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteAccountUseCaseImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserHashPassword userHashPassword;

    @Mock
    private AccountDeletionDataRepository accountDeletionDataRepository;

    @Mock
    private StravaOAuthProvider stravaOAuthProvider;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private DeleteAccountUseCaseImpl useCase;

    @Test
    void execute_deletes_account_owned_data_and_user_when_current_password_is_valid() {
        stubTransaction();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(timeProvider.now()).thenReturn(NOW);

        useCase.execute(deleteAccountCommand());

        InOrder inOrder = inOrder(accountDeletionDataRepository, userRepository);
        inOrder.verify(accountDeletionDataRepository).deleteAccountOwnedData(EMAIL, NOW);
        inOrder.verify(userRepository).deleteByEmail(EMAIL);
    }

    @Test
    void execute_throws_when_authenticated_user_does_not_exist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(deleteAccountCommand()))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);
        verifyNoInteractions(userHashPassword, accountDeletionDataRepository, stravaOAuthProvider);
    }

    @Test
    void execute_throws_invalid_credentials_when_current_password_is_wrong() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(deleteAccountCommand()))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoMoreInteractions(userHashPassword, userRepository);
        verifyNoInteractions(accountDeletionDataRepository, stravaOAuthProvider);
    }

    @Test
    void execute_treats_null_current_password_as_empty() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches("", HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new DeleteAccountCommand(EMAIL, null)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(userHashPassword).matches("", HASHED_PASSWORD);
        verifyNoInteractions(accountDeletionDataRepository, stravaOAuthProvider);
    }

    @Test
    void execute_attempts_strava_deauthorization_after_local_deletion() {
        stubTransaction();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(timeProvider.now()).thenReturn(NOW, NOW);
        when(accountDeletionDataRepository.findStravaDeauthorizations(EMAIL))
                .thenReturn(List.of(new StravaDeauthorization(ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT)));

        useCase.execute(deleteAccountCommand());

        InOrder inOrder = inOrder(accountDeletionDataRepository, userRepository, stravaOAuthProvider);
        inOrder.verify(accountDeletionDataRepository).deleteAccountOwnedData(EMAIL, NOW);
        inOrder.verify(userRepository).deleteByEmail(EMAIL);
        inOrder.verify(stravaOAuthProvider).deauthorize(ACCESS_TOKEN);
    }

    @Test
    void execute_refreshes_expired_strava_authorization_before_deauthorization() {
        stubTransaction();
        StravaTokenAuthorization rotatedAuthorization = new StravaTokenAuthorization(ATHLETE_ID,
                ROTATED_ACCESS_TOKEN, ROTATED_REFRESH_TOKEN, EXPIRES_AT, SCOPE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(timeProvider.now()).thenReturn(NOW, NOW);
        when(accountDeletionDataRepository.findStravaDeauthorizations(EMAIL))
                .thenReturn(List.of(new StravaDeauthorization(ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, NOW)));
        when(stravaOAuthProvider.refresh(REFRESH_TOKEN)).thenReturn(rotatedAuthorization);

        useCase.execute(deleteAccountCommand());

        verify(stravaOAuthProvider).deauthorize(ROTATED_ACCESS_TOKEN);
    }

    @Test
    void execute_does_not_block_local_deletion_when_strava_deauthorization_fails() {
        stubTransaction();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(timeProvider.now()).thenReturn(NOW, NOW);
        when(accountDeletionDataRepository.findStravaDeauthorizations(EMAIL))
                .thenReturn(List.of(new StravaDeauthorization(ATHLETE_ID, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_AT)));
        doThrow(new RuntimeException("strava unavailable")).when(stravaOAuthProvider).deauthorize(ACCESS_TOKEN);

        useCase.execute(deleteAccountCommand());

        verify(userRepository).deleteByEmail(EMAIL);
    }

    private void stubTransaction() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);

            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }
}
