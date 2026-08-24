package com.sudolife.application.service.user;

import com.sudolife.application.model.user.PasswordRecoveryToken;
import com.sudolife.application.model.user.User;
import com.sudolife.application.service.strava.ports.required.TimeProvider;
import com.sudolife.application.service.user.ports.provided.StartPasswordRecoveryUseCase;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryMailSender;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenProvider;
import com.sudolife.application.service.user.ports.required.PasswordRecoveryTokenRepository;
import com.sudolife.application.service.user.ports.required.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StartPasswordRecoveryUseCaseImpl implements StartPasswordRecoveryUseCase {

    private static final Duration TOKEN_DURATION = Duration.ofMinutes(30);
    private static final String GENERIC_MESSAGE = "Se uma conta existir para esse email, instruções de recuperação de senha serão enviadas.";

    private final UserRepository userRepository;
    private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    private final PasswordRecoveryTokenProvider passwordRecoveryTokenProvider;
    private final PasswordRecoveryMailSender passwordRecoveryMailSender;
    private final TimeProvider timeProvider;

    @Override
    public PasswordRecoveryStartResult execute(StartPasswordRecoveryCommand command) {
        Optional<User> user = userRepository.findByEmail(command.email());
        user.ifPresent(this::startRecovery);

        return new PasswordRecoveryStartResult(GENERIC_MESSAGE);
    }

    private void startRecovery(User user) {
        Instant now = timeProvider.now();
        IssuedPasswordRecoveryToken issuedToken = passwordRecoveryTokenProvider.provide();
        PasswordRecoveryToken recoveryToken = new PasswordRecoveryToken(
                null,
                user.getEmail().value(),
                issuedToken.tokenHash(),
                now.plus(TOKEN_DURATION),
                null,
                now
        );

        passwordRecoveryTokenRepository.invalidateActiveTokens(user.getEmail().value(), now);
        passwordRecoveryTokenRepository.save(recoveryToken);
        passwordRecoveryMailSender.send(new PasswordRecoveryEmail(user.getEmail().value(), issuedToken.rawToken()));
    }
}
