package com.sudolife.application.service.user;

import com.sudolife.application.service.user.exception.AuthenticatedUserNotFoundException;
import com.sudolife.application.service.user.ports.required.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.sudolife.helper.UserTestHelper.EMAIL;
import static com.sudolife.helper.UserTestHelper.NAME;
import static com.sudolife.helper.UserTestHelper.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetCurrentUserUseCaseImpl useCase;

    @Test
    void execute_returns_current_user_when_user_exists() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));

        CurrentUserResult result = useCase.execute(EMAIL);

        assertThat(result).isEqualTo(new CurrentUserResult(1L, NAME, EMAIL));
    }

    @Test
    void execute_throws_when_authenticated_user_does_not_exist() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(EMAIL))
                .isInstanceOf(AuthenticatedUserNotFoundException.class);
    }
}
