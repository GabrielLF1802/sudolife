package com.sudolife.application.service.user;

import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.exception.UserAlreadyExistsException;
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
import static com.sudolife.helper.UserTestHelper.NAME;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static com.sudolife.helper.UserTestHelper.registerUserCommand;
import static com.sudolife.helper.UserTestHelper.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserHashPassword userHashPassword;

    @InjectMocks
    private RegisterUserUseCaseImpl useCase;

    @Test
    void execute_saves_user_with_hashed_password() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userHashPassword.hash(PASSWORD)).thenReturn(HASHED_PASSWORD);

        useCase.execute(registerUserCommand());

        User savedUser = capturedSavedUser();
        assertThat(savedUser.getName()).isEqualTo(NAME);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo(HASHED_PASSWORD);
    }

    @Test
    void execute_throws_when_email_already_exists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> useCase.execute(registerUserCommand()))
                .isInstanceOf(UserAlreadyExistsException.class);
        verifyNoMoreInteractions(userHashPassword);
    }

    private User capturedSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }
}
