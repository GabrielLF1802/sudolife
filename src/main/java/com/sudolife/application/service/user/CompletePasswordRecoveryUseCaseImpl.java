package com.sudolife.application.service.user;

import com.sudolife.application.model.user.HashedPassword;
import com.sudolife.application.model.user.NewPassword;
import com.sudolife.application.model.user.PasswordContext;
import com.sudolife.application.model.user.PasswordRecoveryToken;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.user.exception.InvalidPasswordRecoveryTokenException;
import com.sudolife.application.service.user.exception.NewPasswordMatchesCurrentPasswordException;
import com.sudolife.application.service.user.ports.provided.CompletePasswordRecoveryUseCase;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenProvider;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenRepository;
import com.sudolife.application.service.user.ports.required.UserHashPassword;
import com.sudolife.application.service.user.ports.required.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CompletePasswordRecoveryUseCaseImpl implements CompletePasswordRecoveryUseCase {

    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final PasswordRecoveryTokenProvider passwordRecoveryTokenProvider;
    private final UserRepository userRepository;
    private final UserHashPassword userHashPassword;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public void execute(CompletePasswordRecoveryCommand command) {
        Instant now = timeProvider.now();
        PasswordRecoveryToken recoveryToken = findUsableToken(command, now);
        User user = userRepository.findByEmail(recoveryToken.getUserEmail())
                .orElseThrow(InvalidPasswordRecoveryTokenException::new);
        NewPassword newPassword = new NewPassword(
                command.newPassword(),
                PasswordContext.registration(user.getEmail().value(), user.getName())
        );

        if (userHashPassword.matches(newPassword.value(), user.getPassword().value())) {
            throw new NewPasswordMatchesCurrentPasswordException();
        }

        user.changePassword(new HashedPassword(userHashPassword.hash(newPassword.value())));
        userRepository.save(user);
        passwordRecoveryTokenRepository.save(recoveryToken.consume(now));
    }

    private PasswordRecoveryToken findUsableToken(CompletePasswordRecoveryCommand command, Instant now) {
        String tokenHash = passwordRecoveryTokenProvider.hash(rawToken(command.token()));
        PasswordRecoveryToken recoveryToken = passwordRecoveryTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidPasswordRecoveryTokenException::new);

        if (!recoveryToken.canBeUsedAt(now)) {
            throw new InvalidPasswordRecoveryTokenException();
        }

        return recoveryToken;
    }

    private String rawToken(String value) {
        return value == null ? "" : value;
    }
}
