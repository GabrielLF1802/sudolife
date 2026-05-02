package com.sudolife.application.service.user;

import com.sudolife.application.service.user.exception.InvalidCredentialsException;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import com.sudolife.application.service.user.ports.required.UserToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.sudolife.helper.UserTestHelper.EMAIL;
import static com.sudolife.helper.UserTestHelper.HASHED_PASSWORD;
import static com.sudolife.helper.UserTestHelper.PASSWORD;
import static com.sudolife.helper.UserTestHelper.TOKEN;
import static com.sudolife.helper.UserTestHelper.authenticateUserCommand;
import static com.sudolife.helper.UserTestHelper.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserToken userToken;

    @Mock
    private UserHashPassword userHashPassword;

    @InjectMocks
    private AuthenticateUserUseCaseImpl useCase;

    @Test
    void execute_returns_token_when_credentials_are_valid() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(userToken.generateToken(any())).thenReturn(TOKEN);

        AuthenticationResult result = useCase.execute(authenticateUserCommand());

        assertThat(result.token()).isEqualTo(TOKEN);
    }

    @Test
    void execute_throws_when_user_does_not_exist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(authenticateUserCommand()))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(userHashPassword, userToken);
    }

    @Test
    void execute_throws_when_password_is_invalid() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(userHashPassword.matches(PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(authenticateUserCommand()))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(userToken);
    }
}
