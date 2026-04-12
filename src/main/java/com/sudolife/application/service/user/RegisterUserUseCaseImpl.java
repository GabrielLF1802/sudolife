package com.sudolife.application.service.user;

import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.provided.RegisterUserUseCase;
import com.sudolife.application.service.user.required.UserHashPassword;
import com.sudolife.application.service.user.required.UserRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {
    private final UserRepository userRepository;
    private final UserHashPassword userHashPassword;

    @Override
    public User execute(User user) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("This Email already exists");
        }

        String hashedPassword = userHashPassword.hash(user.getPassword());
        user.setPassword(hashedPassword);

        return userRepository.save(user);
    }
}
