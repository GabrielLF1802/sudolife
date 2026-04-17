package com.sudolife.application.service.user;

import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.ports.provided.RegisterUserUseCase;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
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
