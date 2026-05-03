package com.sudolife.application.service.user;

import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.exception.AuthenticatedUserNotFoundException;
import com.sudolife.application.service.user.ports.provided.GetCurrentUserUseCase;
import com.sudolife.application.service.user.ports.required.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCurrentUserUseCaseImpl implements GetCurrentUserUseCase {

    private final UserRepository userRepository;

    @Override
    public CurrentUserResult execute(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        return new CurrentUserResult(user.getId(), user.getName(), user.getEmail().value());
    }
}
