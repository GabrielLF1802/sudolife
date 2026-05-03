package com.sudolife.application.service.user;

import com.sudolife.application.model.user.Email;
import com.sudolife.application.model.user.HashedPassword;
import com.sudolife.application.model.user.RawPassword;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.exception.UserAlreadyExistsException;
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
    public void execute(RegisterUserCommand command) {
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new UserAlreadyExistsException();
        }

        RawPassword rawPassword = new RawPassword(command.password());
        String hashedPassword = userHashPassword.hash(rawPassword.value());
        User user = new User(null, command.name(), new Email(command.email()), new HashedPassword(hashedPassword));

        userRepository.save(user);
    }
}
