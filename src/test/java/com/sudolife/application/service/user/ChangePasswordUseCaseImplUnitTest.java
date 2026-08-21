package com.sudolife.application.service.user;

import com.sudolife.application.model.user.InvalidPasswordException;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.exception.AuthenticatedUserNotFoundException;
import com.sudolife.application.service.user.exception.InvalidCredentialsException;
import com.sudolife.application.service.user.exception.NewPasswordMatchesCurrentPasswordException;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.sudolife.helper.UserTestHelper.EMAIL;
import static com.sudolife.helper.UserTestHelper.HASHED_PASSWORD;
import static com.sudolife.helper.UserTestHelper.NEW_HASHED_PASSWORD;
import static com.sudolife.helper.UserTestHelper.NEW_PASSWORD;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static com.sudolife.helper.UserTestHelper.changePasswordCommand;
import static com.sudolife.helper.UserTestHelper.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserHashPassword userHashPassword;

    @InjectMocks
    private ChangePasswordUseCaseImpl useCase;

    @Test
    void execute_updates_user_with_hashed_new_password_when_current_password_is_valid() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(userHashPassword.matches(NEW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);
        when(userHashPassword.hash(NEW_PASSWORD)).thenReturn(NEW_HASHED_PASSWORD);

        useCase.execute(changePasswordCommand());

        User savedUser = capturedSavedUser();
        assertThat(savedUser.getPassword().value()).isEqualTo(NEW_HASHED_PASSWORD);
    }

    @Test
    void execute_throws_when_authenticated_user_does_not_exist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(changePasswordCommand()))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);
        verifyNoInteractions(userHashPassword);
    }

    @Test
    void execute_throws_invalid_credentials_when_current_password_is_wrong() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(changePasswordCommand()))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoMoreInteractions(userHashPassword, userRepository);
    }

    @Test
    void execute_throws_when_new_password_is_weak() {
        ChangePasswordCommand command = new ChangePasswordCommand(EMAIL, PASSWORD, "weak");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidPasswordException.class);
        verifyNoMoreInteractions(userHashPassword, userRepository);
    }

    @Test
    void execute_throws_when_new_password_matches_current_password() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new ChangePasswordCommand(EMAIL, PASSWORD, PASSWORD)))
                .isInstanceOf(NewPasswordMatchesCurrentPasswordException.class);
        verifyNoMoreInteractions(userHashPassword, userRepository);
    }

    private User capturedSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }
}
