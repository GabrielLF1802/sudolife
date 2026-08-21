package com.sudolife.application.service.user;

import com.sudolife.application.model.user.HashedPassword;
import com.sudolife.application.model.user.NewPassword;
import com.sudolife.application.model.user.PasswordContext;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.user.exception.AuthenticatedUserNotFoundException;
import com.sudolife.application.service.user.exception.InvalidCredentialsException;
import com.sudolife.application.service.user.exception.NewPasswordMatchesCurrentPasswordException;
import com.sudolife.application.service.user.ports.provided.ChangePasswordUseCase;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCaseImpl implements ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final UserHashPassword userHashPassword;

    @Override
    public void execute(ChangePasswordCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        if (!currentPasswordMatches(command, user)) {
            throw new InvalidCredentialsException();
        }

        NewPassword newPassword = new NewPassword(
                command.newPassword(),
                PasswordContext.registration(user.getEmail().value(), user.getName())
        );

        if (userHashPassword.matches(newPassword.value(), user.getPassword().value())) {
            throw new NewPasswordMatchesCurrentPasswordException();
        }

        user.changePassword(new HashedPassword(userHashPassword.hash(newPassword.value())));
        userRepository.save(user);
    }

    private boolean currentPasswordMatches(ChangePasswordCommand command, User user) {
        return userHashPassword.matches(rawValue(command.currentPassword()), user.getPassword().value());
    }

    private String rawValue(String value) {
        return value == null ? "" : value;
    }
}
