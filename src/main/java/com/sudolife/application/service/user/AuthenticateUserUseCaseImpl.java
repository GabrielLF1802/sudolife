package com.sudolife.application.service.user;

import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.ports.provided.AuthenticateUserUseCase;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import com.sudolife.application.service.user.ports.required.UserToken;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final UserToken userToken;
    private final UserHashPassword userHashPassword;

    @Override
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found "));

        if (!userHashPassword.matches(password, user.getPassword())) throw new RuntimeException("Password is invalid");

        return userToken.generateToken(user);
    }
}
